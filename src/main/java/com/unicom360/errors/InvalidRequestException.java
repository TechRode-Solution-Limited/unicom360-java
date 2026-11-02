package com.unicom360.errors;

/** HTTP 400: the request's shape is wrong. Nothing was sent. Never retried. */
public final class InvalidRequestException extends ApiStatusException {
  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param message the gateway's message, or an SDK-generated one
   * @param details what the response said
   * @param attempts the HTTP attempts made, retries included
   */
  public InvalidRequestException(String message, ErrorDetails details, int attempts) {
    super(message, details, attempts);
  }

  /**
   * Returns {@code invalid_request_error}.
   *
   * @return the type string
   */
  @Override
  public String type() {
    return "invalid_request_error";
  }
}
