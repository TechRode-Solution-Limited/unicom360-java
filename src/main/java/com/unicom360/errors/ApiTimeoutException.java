package com.unicom360.errors;

/** The connection or the response timed out. */
public final class ApiTimeoutException extends ApiConnectionException {
  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param message what happened
   * @param cause the underlying exception, or {@code null}
   * @param sent whether any byte of the request may have reached the server ({@code false} for a
   *     connect timeout)
   * @param attempts the HTTP attempts made, retries included
   */
  public ApiTimeoutException(String message, Throwable cause, boolean sent, int attempts) {
    super(message, cause, sent, attempts);
  }

  /**
   * Returns {@code timeout_error}.
   *
   * @return the type string
   */
  @Override
  public String type() {
    return "timeout_error";
  }
}
