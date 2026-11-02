package com.unicom360.transport;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One HTTP request, as the client built it. {@link #toString()} leaves out the headers, which carry
 * the API key.
 *
 * @param method {@code GET} or {@code POST}
 * @param uri the absolute URL
 * @param headers the headers to send, in order (including {@code X-API-Key}: never log them)
 * @param body the UTF-8 JSON body, or {@code null} for a request without one
 * @param timeout the time allowed from sending the request to receiving the whole body
 * @param connectTimeout the time allowed to connect, TLS included
 */
public record TransportRequest(
    String method,
    URI uri,
    Map<String, String> headers,
    byte[] body,
    Duration timeout,
    Duration connectTimeout) {

  /**
   * Copies the headers and the body, so the record is immutable.
   *
   * @param method {@code GET} or {@code POST}
   * @param uri the absolute URL
   * @param headers the headers to send, in order (including {@code X-API-Key}: never log them)
   * @param body the UTF-8 JSON body, or {@code null} for a request without one
   * @param timeout the time allowed from sending the request to receiving the whole body
   * @param connectTimeout the time allowed to connect, TLS included
   */
  public TransportRequest {
    headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
    body = body == null ? null : body.clone();
  }

  /**
   * The body bytes.
   *
   * @return a copy of the body, or {@code null}
   */
  @Override
  public byte[] body() {
    return body == null ? null : body.clone();
  }

  /**
   * The body as text.
   *
   * @return the body decoded as UTF-8, or {@code null}
   */
  public String bodyText() {
    return body == null ? null : new String(body, StandardCharsets.UTF_8);
  }

  /**
   * The value of a header, matched without regard to case.
   *
   * @param name the header name
   * @return the value, or {@code null}
   */
  public String header(String name) {
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      if (entry.getKey().equalsIgnoreCase(name)) {
        return entry.getValue();
      }
    }
    return null;
  }

  /**
   * Compares every component, the body by content.
   *
   * @param other the object to compare with
   * @return whether they are equal
   */
  @Override
  public boolean equals(Object other) {
    return other instanceof TransportRequest that
        && method.equals(that.method)
        && uri.equals(that.uri)
        && headers.equals(that.headers)
        && java.util.Arrays.equals(body, that.body)
        && timeout.equals(that.timeout)
        && connectTimeout.equals(that.connectTimeout);
  }

  /**
   * A hash of every component, the body by content.
   *
   * @return the hash
   */
  @Override
  public int hashCode() {
    return java.util.Objects.hash(method, uri, headers, java.util.Arrays.hashCode(body), timeout);
  }

  /**
   * The method, URL and body size; never the headers.
   *
   * @return a description without secrets
   */
  @Override
  public String toString() {
    return "TransportRequest[method="
        + method
        + ", uri="
        + uri
        + ", headers=[REDACTED], body="
        + (body == null ? "none" : body.length + " bytes")
        + ", timeout="
        + timeout
        + "]";
  }
}
