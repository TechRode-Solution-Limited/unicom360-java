package com.unicom360.errors;

/**
 * The bundle does not cover the send ({@code INSUFFICIENT_BUNDLE}, normalised from {@code
 * Insufficient bundle}, {@code INSUFFICIENT_BUNDLE_LIMIT} and {@code INSUFFICIENT_BUNDLE: no
 * international SMS bundle}). Top up, then send again.
 */
public final class InsufficientBundleException extends ApiException {
  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param message the gateway's message
   * @param details what the response said
   * @param attempts the HTTP attempts made, retries included
   */
  public InsufficientBundleException(String message, ErrorDetails details, int attempts) {
    super(message, details, attempts);
  }
}
