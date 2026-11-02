package com.unicom360.transport;

/**
 * A request that produced no complete response. The two facts decide whether the client may retry
 * it: a request that certainly never reached the server ({@code sent == false}) is always retried,
 * any other only when the call is retry-safe.
 */
public class TransportException extends Exception {
  private static final long serialVersionUID = 1L;

  /** Whether any byte of the request may have reached the server. */
  private final boolean sent;

  /** Whether the failure was a timeout. */
  private final boolean timeout;

  /**
   * Creates the exception.
   *
   * @param message what happened (never include the API key)
   * @param sent whether any byte of the request may have reached the server; when the underlying
   *     library cannot tell, pass {@code true}
   * @param timeout whether the failure was a timeout
   */
  public TransportException(String message, boolean sent, boolean timeout) {
    this(message, sent, timeout, null);
  }

  /**
   * Creates the exception.
   *
   * @param message what happened (never include the API key)
   * @param sent whether any byte of the request may have reached the server; when the underlying
   *     library cannot tell, pass {@code true}
   * @param timeout whether the failure was a timeout
   * @param cause the underlying exception, or {@code null}
   */
  public TransportException(String message, boolean sent, boolean timeout, Throwable cause) {
    super(message, cause);
    this.sent = sent;
    this.timeout = timeout;
  }

  /**
   * Whether any byte of the request may have reached the server.
   *
   * @return {@code false} only when the request certainly did not
   */
  public boolean sent() {
    return sent;
  }

  /**
   * Whether the failure was a timeout.
   *
   * @return whether it timed out
   */
  public boolean timeout() {
    return timeout;
  }
}
