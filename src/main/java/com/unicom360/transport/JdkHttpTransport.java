package com.unicom360.transport;

import com.unicom360.errors.ConfigurationException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.channels.UnresolvedAddressException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

/**
 * The default transport, on {@code java.net.http.HttpClient}. It never follows redirects, bounds
 * each attempt by the request's timeout from sending to the end of the body, and classifies every
 * failure for the retry rules.
 *
 * <p>To configure the HTTP client yourself (a proxy, an {@code SSLContext}, an executor), build one
 * that never follows redirects and pass it to {@link #of(HttpClient)} or to {@code
 * Unicom360Client.builder().httpClient(...)}. Its own connect timeout then applies.
 */
public final class JdkHttpTransport implements Transport {
  private static final int MAX_CAUSE_DEPTH = 32;

  private final HttpClient client;

  private JdkHttpTransport(HttpClient client) {
    this.client = client;
  }

  /**
   * A transport on a new {@code HttpClient} with the given connect timeout.
   *
   * @param connectTimeout the time allowed to connect, TLS included
   * @return the transport
   */
  public static JdkHttpTransport create(Duration connectTimeout) {
    return new JdkHttpTransport(
        HttpClient.newBuilder()
            .connectTimeout(Objects.requireNonNull(connectTimeout, "connectTimeout"))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build());
  }

  /**
   * A transport on your own {@code HttpClient}.
   *
   * @param client the client; it must not follow redirects
   * @return the transport
   * @throws ConfigurationException when the client follows redirects, which would replay the API
   *     key to another host
   */
  public static JdkHttpTransport of(HttpClient client) {
    Objects.requireNonNull(client, "client");
    if (client.followRedirects() != HttpClient.Redirect.NEVER) {
      throw new ConfigurationException(
          "The HttpClient must not follow redirects (HttpClient.Redirect.NEVER): a redirect would"
              + " send the API key to another host.");
    }
    return new JdkHttpTransport(client);
  }

  /**
   * The underlying HTTP client.
   *
   * @return the client
   */
  public HttpClient httpClient() {
    return client;
  }

  @Override
  public TransportResponse send(TransportRequest request) throws TransportException {
    HttpRequest.Builder builder = HttpRequest.newBuilder(request.uri()).timeout(request.timeout());
    if ("http".equalsIgnoreCase(request.uri().getScheme())) {
      // No h2c upgrade attempt on plain http (a local mock).
      builder.version(HttpClient.Version.HTTP_1_1);
    }
    for (Map.Entry<String, String> header : request.headers().entrySet()) {
      builder.header(header.getKey(), header.getValue());
    }
    byte[] body = request.body();
    builder.method(
        request.method(),
        body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofByteArray(body));

    CompletableFuture<HttpResponse<String>> future =
        client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString());
    HttpResponse<String> response;
    try {
      response = future.get(request.timeout().toNanos(), TimeUnit.NANOSECONDS);
    } catch (TimeoutException e) {
      future.cancel(true);
      throw new TransportException(
          "No complete response within " + request.timeout(), true, true, e);
    } catch (ExecutionException e) {
      throw classify(e.getCause() == null ? e : e.getCause());
    } catch (CancellationException e) {
      throw new TransportException("The request was cancelled", true, false, e);
    } catch (InterruptedException e) {
      future.cancel(true);
      Thread.currentThread().interrupt();
      throw new TransportException("Interrupted while waiting for the response", true, false, e);
    }
    return new TransportResponse(response.statusCode(), response.headers().map(), response.body());
  }

  /** Classifies a failure of the JDK client (docs/sdk-design.md section 3.4). */
  static TransportException classify(Throwable failure) {
    String message = "The request to the Unicom360 API failed: " + describe(failure);
    Throwable cause = failure;
    for (int depth = 0; cause != null && depth < MAX_CAUSE_DEPTH; depth++) {
      if (cause instanceof HttpConnectTimeoutException) {
        return new TransportException(message, false, true, failure);
      }
      if (cause instanceof HttpTimeoutException) {
        return new TransportException(message, true, true, failure);
      }
      if (cause instanceof ConnectException
          || cause instanceof UnknownHostException
          || cause instanceof UnresolvedAddressException
          || cause instanceof NoRouteToHostException
          || cause instanceof SSLHandshakeException
          || cause instanceof SSLPeerUnverifiedException) {
        return new TransportException(message, false, false, failure);
      }
      cause = cause.getCause();
    }
    // Anything else may have happened after the request was written.
    return new TransportException(message, true, false, failure);
  }

  private static String describe(Throwable failure) {
    String text = failure.getMessage();
    return failure.getClass().getSimpleName() + (text == null ? "" : " (" + text + ")");
  }
}
