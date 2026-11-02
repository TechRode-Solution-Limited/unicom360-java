package com.unicom360.transport;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * One HTTP response.
 *
 * @param status the HTTP status code
 * @param headers the headers, names in lower case
 * @param body the body text ({@code ""} for none)
 */
public record TransportResponse(int status, Map<String, List<String>> headers, String body) {

  /**
   * Lower-cases the header names and copies them; a {@code null} body becomes {@code ""}.
   *
   * @param status the HTTP status code
   * @param headers the headers, names in lower case
   * @param body the body text ({@code ""} for none)
   */
  public TransportResponse {
    Map<String, List<String>> copy = new TreeMap<>();
    if (headers != null) {
      headers.forEach((name, values) -> {
        if (name != null) {
          copy.merge(
              name.toLowerCase(Locale.ROOT),
              values == null ? List.of() : List.copyOf(values),
              (a, b) -> {
                List<String> merged = new java.util.ArrayList<>(a);
                merged.addAll(b);
                return List.copyOf(merged);
              });
        }
      });
    }
    headers = Collections.unmodifiableMap(copy);
    body = body == null ? "" : body;
  }

  /**
   * A response without headers.
   *
   * @param status the HTTP status code
   * @param body the body text
   * @return the response
   */
  public static TransportResponse of(int status, String body) {
    return new TransportResponse(status, Map.of(), body);
  }

  /**
   * The first value of a header, matched without regard to case.
   *
   * @param name the header name
   * @return the value, or {@code null}
   */
  public String header(String name) {
    List<String> values = headers.get(name.toLowerCase(Locale.ROOT));
    return values == null || values.isEmpty() ? null : values.get(0);
  }
}
