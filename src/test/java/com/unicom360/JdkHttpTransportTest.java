package com.unicom360;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.unicom360.errors.ApiConnectionException;
import com.unicom360.errors.ApiStatusException;
import com.unicom360.errors.ApiTimeoutException;
import com.unicom360.errors.Unicom360Exception;
import com.unicom360.models.Balances;
import com.unicom360.params.SmsSendParams;
import com.unicom360.transport.JdkHttpTransport;
import com.unicom360.transport.TransportException;
import com.unicom360.transport.TransportRequest;
import com.unicom360.transport.TransportResponse;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The default transport against a real socket on the loopback interface. */
class JdkHttpTransportTest {
  private static final String BALANCE =
      "{\"status\":true,\"message\":\"m\",\"balances\":{\"SMS\":1250},\"client_id\":\"c\",\"client_name\":\"n\"}";

  /** What the fake server does with each accepted request. */
  @FunctionalInterface
  private interface Behaviour {
    void handle(Socket socket, String request) throws IOException, InterruptedException;
  }

  private ServerSocket server;
  private Thread thread;
  private final List<String> received = new CopyOnWriteArrayList<>();
  private final CountDownLatch release = new CountDownLatch(1);

  private String serve(Behaviour behaviour) throws IOException {
    server = new ServerSocket(0, 16, InetAddress.getLoopbackAddress());
    thread =
        new Thread(() -> {
          while (!server.isClosed()) {
            try (Socket socket = server.accept()) {
              String request = read(socket.getInputStream());
              received.add(request);
              behaviour.handle(socket, request);
            } catch (IOException | InterruptedException e) {
              return;
            }
          }
        });
    thread.setDaemon(true);
    thread.start();
    return "http://127.0.0.1:" + server.getLocalPort();
  }

  @AfterEach
  void stop() throws IOException {
    release.countDown();
    if (server != null) {
      server.close();
    }
  }

  private static String read(InputStream raw) throws IOException {
    InputStream in = new BufferedInputStream(raw);
    ByteArrayOutputStream head = new ByteArrayOutputStream();
    while (true) {
      int b = in.read();
      if (b < 0) {
        return head.toString(StandardCharsets.UTF_8);
      }
      head.write(b);
      byte[] bytes = head.toByteArray();
      int n = bytes.length;
      if (n >= 4
          && bytes[n - 4] == '\r'
          && bytes[n - 3] == '\n'
          && bytes[n - 2] == '\r'
          && bytes[n - 1] == '\n') {
        break;
      }
    }
    String text = head.toString(StandardCharsets.UTF_8);
    int length = 0;
    for (String line : text.split("\r\n")) {
      if (line.toLowerCase(Locale.ROOT).startsWith("content-length:")) {
        length = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
      }
    }
    return text + new String(in.readNBytes(length), StandardCharsets.UTF_8);
  }

  private static void respond(Socket socket, int status, String headers, String body)
      throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    String head =
        "HTTP/1.1 "
            + status
            + " X\r\n"
            + headers
            + "Content-Length: "
            + bytes.length
            + "\r\nConnection: close\r\n\r\n";
    socket.getOutputStream().write(head.getBytes(StandardCharsets.US_ASCII));
    socket.getOutputStream().write(bytes);
    socket.getOutputStream().flush();
  }

  private static Unicom360Client client(String baseUrl) {
    return Unicom360Client.builder()
        .apiKey("the-key")
        .baseUrl(baseUrl)
        .timeout(Duration.ofMillis(500))
        .connectTimeout(Duration.ofSeconds(2))
        .maxRetries(0)
        .build();
  }

  private static SmsSendParams sms() {
    return SmsSendParams.builder().from("U").to("260971234567").message("Héllo €").build();
  }

  @Test
  void itSendsTheRequestAndReadsTheResponse() throws IOException {
    String url =
        serve((socket, request) ->
            respond(socket, 200, "Content-Type: application/json\r\n", BALANCE));
    Balances balances =
        Unicom360Client.builder().apiKey("the-key").baseUrl(url).build().balance().all();
    assertEquals(1, balances.balances().get("SMS").intValueExact() / 1250);
    String request = received.get(0);
    String lower = request.toLowerCase(Locale.ROOT);
    assertTrue(request.startsWith("POST /api/v1/json/balance/all HTTP/1.1\r\n"), request);
    assertTrue(lower.contains("x-api-key: the-key\r\n"), request);
    assertTrue(lower.contains("accept: application/json\r\n"), request);
    assertTrue(lower.contains("content-type: application/json\r\n"), request);
    assertTrue(lower.contains("user-agent: unicom360-java/"), request);
    assertFalse(lower.contains("authorization:"), request);
    assertFalse(lower.contains("upgrade:"), "no h2c upgrade on plain http: " + request);
    assertTrue(request.endsWith("{}"), request);
  }

  @Test
  void bodiesAreUtf8() throws IOException {
    String url =
        serve((socket, request) ->
            respond(socket, 200, "", "{\"success\":true,\"message\":\"é\",\"data\":[]}"));
    assertEquals("é", client(url).sms().send(sms()).message());
    assertTrue(received.get(0).contains("Héllo €"), received.get(0));
  }

  @Test
  void aGetHasNoBody() throws IOException {
    String url = serve((socket, request) -> respond(socket, 200, "", "{\"status\":\"ok\"}"));
    assertEquals("ok", client(url).health().status());
    String request = received.get(0);
    assertTrue(request.startsWith("GET /health HTTP/1.1\r\n"), request);
    assertFalse(request.toLowerCase(Locale.ROOT).contains("x-api-key"), request);
    assertTrue(request.endsWith("\r\n\r\n"), request);
  }

  @Test
  void aRedirectIsNeverFollowed() throws IOException {
    String url =
        serve((socket, request) ->
            respond(socket, 302, "Location: http://127.0.0.1:9/stolen\r\n", ""));
    ApiStatusException error =
        assertThrows(ApiStatusException.class, () -> client(url).balance().all());
    assertEquals(302, error.httpStatus());
    assertEquals("api_status_error", error.type());
    assertEquals("HTTP 302 Found", error.getMessage());
    assertEquals(List.of("http://127.0.0.1:9/stolen"), error.headers().get("location"));
    assertEquals(1, received.size(), "the redirect was not followed");
  }

  @Test
  void aRefusedConnectionWasNotSent() throws IOException {
    int port;
    try (ServerSocket closed = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      port = closed.getLocalPort();
    }
    String url = "http://127.0.0.1:" + port;
    ApiConnectionException error =
        assertThrows(ApiConnectionException.class, () -> client(url).sms().send(sms()));
    assertFalse(error.sent(), error.getMessage());
    assertEquals("connection_error", error.type());
    assertNotNull(error.getCause());
  }

  @Test
  void aRefusedConnectionIsRetriedEvenForAnUnsafeSend() throws IOException {
    int port;
    try (ServerSocket closed = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      port = closed.getLocalPort();
    }
    List<Duration> sleeps = new ArrayList<>();
    Unicom360Client client =
        client("http://127.0.0.1:" + port).withOptions(o -> o.maxRetries(2).sleeper(sleeps::add));
    Unicom360Exception error =
        assertThrows(Unicom360Exception.class, () -> client.sms().send(sms()));
    assertEquals(3, error.attempts());
    assertEquals(2, sleeps.size());
  }

  @Test
  void noResponseWithinTheTimeoutWasSentAndTimedOut() throws IOException {
    String url = serve((socket, request) -> release.await());
    ApiTimeoutException error =
        assertTimeoutPreemptively(
            Duration.ofSeconds(10),
            () -> assertThrows(ApiTimeoutException.class, () -> client(url).sms().send(sms())));
    assertTrue(error.sent());
    assertEquals("timeout_error", error.type());
    assertInstanceOf(ApiConnectionException.class, error);
  }

  @Test
  void theTimeoutCoversTheWholeBody() throws IOException {
    String url =
        serve((socket, request) -> {
          socket
              .getOutputStream()
              .write(
                  ("HTTP/1.1 200 OK\r\nContent-Length: 1000\r\n\r\n{\"status\":true")
                      .getBytes(StandardCharsets.US_ASCII));
          socket.getOutputStream().flush();
          release.await();
        });
    ApiTimeoutException error =
        assertTimeoutPreemptively(
            Duration.ofSeconds(10),
            () -> assertThrows(ApiTimeoutException.class, () -> client(url).balance().all()));
    assertTrue(error.sent());
  }

  @Test
  void aResetAfterTheRequestMayHaveBeenSent() throws IOException {
    String url = serve((socket, request) -> socket.close());
    ApiConnectionException error =
        assertThrows(ApiConnectionException.class, () -> client(url).sms().send(sms()));
    assertTrue(error.sent(), error.getMessage());
    assertEquals(ApiConnectionException.class, error.getClass());
  }

  @Test
  void anUnknownHostWasNotSent() {
    JdkHttpTransport transport = JdkHttpTransport.create(Duration.ofSeconds(2));
    TransportRequest request =
        new TransportRequest(
            "GET",
            URI.create("https://unicom360-does-not-exist.invalid/health"),
            Map.of(),
            null,
            Duration.ofSeconds(5),
            Duration.ofSeconds(2));
    TransportException failure =
        assertThrows(TransportException.class, () -> transport.send(request));
    assertFalse(failure.sent(), failure.getMessage());
    assertFalse(failure.timeout(), failure.getMessage());
  }

  @Test
  void aResponseKeepsItsHeadersInLowerCase() throws IOException, TransportException {
    String url = serve((socket, request) -> respond(socket, 200, "X-Thing: a\r\n", "ok"));
    JdkHttpTransport transport = JdkHttpTransport.create(Duration.ofSeconds(2));
    TransportResponse response =
        transport.send(
            new TransportRequest(
                "GET",
                URI.create(url + "/"),
                Map.of(),
                null,
                Duration.ofSeconds(5),
                Duration.ofSeconds(2)));
    assertEquals(200, response.status());
    assertEquals("ok", response.body());
    assertEquals("a", response.header("X-THING"));
    assertEquals(List.of("a"), response.headers().get("x-thing"));
    assertNull(response.header("missing"));
  }
}
