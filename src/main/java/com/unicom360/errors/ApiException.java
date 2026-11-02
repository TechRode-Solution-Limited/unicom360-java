package com.unicom360.errors;

import com.google.gson.JsonElement;
import java.util.List;
import java.util.Map;

/**
 * A business refusal: HTTP 200 with {@code success: false} (or {@code status: false} on the balance
 * endpoints). Nothing was sent. {@link #code()} says why; subclasses name the codes worth catching
 * on their own. Retried only for {@code SEND_FAILED} on a retry-safe call.
 */
public sealed class ApiException extends Unicom360Exception
    permits InsufficientBundleException,
        DuplicateReferenceException,
        NoValidRecipientsException,
        SessionWindowClosedException {
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
   * @param message the gateway's message (a code such as {@code INSUFFICIENT_BUNDLE} or a sentence)
   * @param details what the response said
   * @param attempts the HTTP attempts made, retries included
   */
  public ApiException(String message, ErrorDetails details, int attempts) {
    super(message, null, attempts);
    this.details = details;
    this.httpStatus = details.httpStatus();
    this.code = details.code();
    this.apiReference = details.apiReference();
    this.rawBody = details.rawBody();
  }

  /**
   * The HTTP status code (a 2xx, normally 200).
   *
   * @return the status code
   */
  public int httpStatus() {
    return httpStatus;
  }

  /**
   * The error code normalised from the gateway's message ({@link com.unicom360.ErrorCodes}): {@code
   * Invalid From Sender ID} and {@code Invalid Sender} are both {@code INVALID_SENDER_ID}, and an
   * unrecognised message is {@code UNKNOWN}.
   *
   * @return the code
   */
  public String code() {
    return code;
  }

  /**
   * The {@code api_reference} of the request, when the body carried one (the balance endpoints send
   * none): quote it to support.
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
   * Returns {@code api_error} (the subclasses too).
   *
   * @return the type string
   */
  @Override
  public String type() {
    return "api_error";
  }
}
