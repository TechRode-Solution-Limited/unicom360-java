package com.unicom360.errors;

/**
 * The customer's 24-hour WhatsApp service window is closed ({@code SESSION_WINDOW_CLOSED}): send an
 * approved template instead.
 */
public final class SessionWindowClosedException extends ApiException {
  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param message the gateway's message
   * @param details what the response said
   * @param attempts the HTTP attempts made, retries included
   */
  public SessionWindowClosedException(String message, ErrorDetails details, int attempts) {
    super(message, details, attempts);
  }
}
