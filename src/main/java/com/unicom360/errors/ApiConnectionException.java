package com.unicom360.errors;

/**
 * No complete HTTP response arrived: the connection failed, was reset, or the response was
 * malformed. The cause is the transport's exception.
 */
public sealed class ApiConnectionException extends Unicom360Exception permits ApiTimeoutException {
  private static final long serialVersionUID = 1L;

  /** Whether any byte of the request may have reached the server. */
  private final boolean sent;

  /**
   * Creates the exception.
   *
   * @param message what happened
   * @param cause the underlying exception, or {@code null}
   * @param sent whether any byte of the request may have reached the server
   * @param attempts the HTTP attempts made, retries included
   */
  public ApiConnectionException(String message, Throwable cause, boolean sent, int attempts) {
    super(message, cause, attempts);
    this.sent = sent;
  }

  /**
   * Whether any byte of the request may have reached the server. {@code false} means the request
   * certainly did not (DNS failure, connection refused, TLS handshake failure, connect timeout).
   *
   * @return whether the request may have been sent
   */
  public boolean sent() {
    return sent;
  }

  /**
   * Returns {@code connection_error}.
   *
   * @return the type string
   */
  @Override
  public String type() {
    return "connection_error";
  }
}
