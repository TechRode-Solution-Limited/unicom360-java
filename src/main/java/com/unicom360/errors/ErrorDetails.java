package com.unicom360.errors;

import com.google.gson.JsonElement;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * What an HTTP response said about an error: the facts every {@link ApiStatusException} and {@link
 * ApiException} carries. Build one to construct those exceptions in your own tests.
 *
 * @param httpStatus the HTTP status code
 * @param code the normalised error code (see {@link com.unicom360.ErrorCodes}); {@code UNKNOWN}
 *     when {@code null}
 * @param apiReference the {@code api_reference} the body carried, or {@code null}
 * @param rawBody the response body text ({@code ""} when {@code null})
 * @param data the envelope's {@code data}, or {@code null}
 * @param headers the response headers, names in lower case
 */
public record ErrorDetails(
    int httpStatus,
    String code,
    String apiReference,
    String rawBody,
    JsonElement data,
    Map<String, List<String>> headers) {

  /**
   * Normalises the components: copies, lower-case header names, no {@code null} code or body.
   *
   * @param httpStatus the HTTP status code
   * @param code the normalised error code (see {@link com.unicom360.ErrorCodes}); {@code UNKNOWN}
   *     when {@code null}
   * @param apiReference the {@code api_reference} the body carried, or {@code null}
   * @param rawBody the response body text ({@code ""} when {@code null})
   * @param data the envelope's {@code data}, or {@code null}
   * @param headers the response headers, names in lower case
   */
  public ErrorDetails {
    code = code == null ? "UNKNOWN" : code;
    rawBody = rawBody == null ? "" : rawBody;
    data = data == null || data.isJsonNull() ? null : data.deepCopy();
    Map<String, List<String>> copy = new TreeMap<>();
    if (headers != null) {
      headers.forEach((name, values) -> {
        if (name != null) {
          copy.put(name.toLowerCase(Locale.ROOT), values == null ? List.of() : List.copyOf(values));
        }
      });
    }
    headers = Collections.unmodifiableMap(copy);
  }

  /**
   * The envelope's {@code data} as a deep copy, or {@code null}.
   *
   * @return the data, or {@code null}
   */
  @Override
  public JsonElement data() {
    return data == null ? null : data.deepCopy();
  }
}
