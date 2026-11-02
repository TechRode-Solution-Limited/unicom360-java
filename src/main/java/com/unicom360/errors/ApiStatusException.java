package com.unicom360.errors;

import com.google.gson.JsonElement;
import java.util.List;
import java.util.Map;

/**
 * An HTTP status other than 2xx, or a 2xx without a usable Unicom360 envelope. Subclasses name the
 * common statuses; a 3xx (redirects are never followed), 403, 404, 406 or 413 is this class itself.
 */
public sealed class ApiStatusException extends Unicom360Exception
    permits AuthenticationException,
        RateLimitException,
        InvalidRequestException,
        ApiServerException {
  private static final long serialVersionUID = 1L;

  /** The HTTP status code. */
  private final int httpStatus;

  /** The normalised error code. */
  private final String code;

  /** The body's {@code api_reference}, or {@code null}. */
  private final String apiReference;

  /** The response body text. */
  private final String rawBody;

  private final transient ErrorDetails details;

  /**
   * Creates the exception.
   *
   * @param message the gateway's message, or an SDK-generated one (for example {@code HTTP 502 Bad
   *     Gateway})
   * @param details what the response said
   * @param attempts the HTTP attempts made, retries included
   */
  public ApiStatusException(String message, ErrorDetails details, int attempts) {
    super(message, null, attempts);
    this.details = details;
    this.httpStatus = details.httpStatus();
    this.code = details.code();
    this.apiReference = details.apiReference();
    this.rawBody = details.rawBody();
  }

  /**
   * The HTTP status code.
   *
   * @return the status code
   */
  public int httpStatus() {
    return httpStatus;
  }

  /**
   * The error code normalised from the gateway's message ({@link com.unicom360.ErrorCodes}), {@code
   * UNKNOWN} when it is not recognised.
   *
   * @return the code
   */
  public String code() {
    return code;
  }

  /**
   * The {@code api_reference} of the request, when the body carried one: quote it to support.
   *
   * @return the reference, or {@code null}
   */
  public String apiReference() {
    return apiReference;
  }

  /**
   * The response body text.
   *
   * @return the body
   */
  public String rawBody() {
    return rawBody;
  }

  /**
   * The envelope's {@code data}, or {@code null}.
   *
   * @return a copy of the data, or {@code null}
   */
  public JsonElement data() {
    return details == null ? null : details.data();
  }

  /**
   * The response headers, names in lower case.
   *
   * @return the headers
   */
  public Map<String, List<String>> headers() {
    return details == null ? Map.of() : details.headers();
  }

  /**
   * Returns {@code api_status_error}.
   *
   * @return the type string
   */
  @Override
  public String type() {
    return "api_status_error";
  }
}
