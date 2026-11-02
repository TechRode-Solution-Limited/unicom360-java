package com.unicom360.errors;

/** HTTP 500 to 599. Retried only when the call is retry-safe. */
public final class ApiServerException extends ApiStatusException {
  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param message the gateway's message, or an SDK-generated one
   * @param details what the response said
   * @param attempts the HTTP attempts made, retries included
   */
  public ApiServerException(String message, ErrorDetails details, int attempts) {
    super(message, details, attempts);
  }

  /**
   * Returns {@code api_server_error}.
   *
   * @return the type string
   */
  @Override
  public String type() {
    return "api_server_error";
  }
}
