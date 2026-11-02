package com.unicom360.errors;

import java.time.Duration;

/**
 * HTTP 429: the API key is over its requests-per-minute limit. The limit is a fixed UTC-minute
 * window and the gateway sends no {@code Retry-After}, so {@link #retryAfter()} is the time to the
 * next UTC minute. Always retried (a 429 is answered before any processing).
 */
public final class RateLimitException extends ApiStatusException {
  private static final long serialVersionUID = 1L;

  /** The time from the response to the next UTC minute. */
  private final Duration retryAfter;

  /**
   * Creates the exception.
   *
   * @param message the gateway's message
   * @param details what the response said
   * @param retryAfter the time from the response to the next UTC minute
   * @param attempts the HTTP attempts made, retries included
   */
  public RateLimitException(
      String message, ErrorDetails details, Duration retryAfter, int attempts) {
    super(message, details, attempts);
    this.retryAfter = retryAfter;
  }

  /**
   * The time from the response to the next UTC minute, when the window resets: more than 0 and at
   * most 60 seconds.
   *
   * @return the wait
   */
  public Duration retryAfter() {
    return retryAfter;
  }

  /**
   * Returns {@code rate_limit_error}.
   *
   * @return the type string
   */
  @Override
  public String type() {
    return "rate_limit_error";
  }
}
