package com.unicom360.errors;

/**
 * The base of every exception the SDK throws. It is thrown as itself for a webhook or callback body
 * that is not the JSON object it should be.
 *
 * <p>Messages never contain the API key or a webhook secret.
 */
public sealed class Unicom360Exception extends RuntimeException
    permits ConfigurationException,
        ApiConnectionException,
        ApiStatusException,
        ApiException,
        SignatureVerificationException {
  private static final long serialVersionUID = 1L;

  /** The HTTP attempts an operation made before throwing; 0 when no operation threw it. */
  private final int attempts;

  /**
   * Creates an exception with a message.
   *
   * @param message what went wrong
   */
  public Unicom360Exception(String message) {
    this(message, null, 0);
  }

  /**
   * Creates an exception with a message and a cause.
   *
   * @param message what went wrong
   * @param cause the underlying exception, or {@code null}
   */
  public Unicom360Exception(String message, Throwable cause) {
    this(message, cause, 0);
  }

  /**
   * Creates an exception thrown by an operation.
   *
   * @param message what went wrong
   * @param cause the underlying exception, or {@code null}
   * @param attempts the HTTP attempts made, retries included (0 when no operation threw it)
   */
  protected Unicom360Exception(String message, Throwable cause, int attempts) {
    super(message, cause);
    this.attempts = attempts;
  }

  /**
   * The error's type string, shared by every Unicom360 SDK: {@code unicom360_error} here.
   *
   * @return the type string
   */
  public String type() {
    return "unicom360_error";
  }

  /**
   * The HTTP attempts the operation made, retries included: 1 when it was not retried, 0 when the
   * exception was not thrown by an operation (configuration, webhooks).
   *
   * @return the number of attempts
   */
  public int attempts() {
    return attempts;
  }
}
