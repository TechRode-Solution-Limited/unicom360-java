package com.unicom360.errors;

/**
 * HTTP 401: authentication failed (a missing, unknown, disabled or expired key, or an IP address
 * that is not allowed). Never retried.
 */
public final class AuthenticationException extends ApiStatusException {
  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param message the gateway's message, or an SDK-generated one
   * @param details what the response said
   * @param attempts the HTTP attempts made, retries included
   */
  public AuthenticationException(String message, ErrorDetails details, int attempts) {
    super(message, details, attempts);
  }

  /**
   * Returns {@code authentication_error}.
   *
   * @return the type string
   */
  @Override
  public String type() {
    return "authentication_error";
  }
}
