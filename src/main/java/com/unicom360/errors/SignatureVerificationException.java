package com.unicom360.errors;

/**
 * A webhook delivery failed verification: answer it with HTTP 401 and do not process it. {@link
 * #reason()} says which check failed.
 */
public final class SignatureVerificationException extends Unicom360Exception {
  private static final long serialVersionUID = 1L;

  /** Why verification failed, in the order the checks run. */
  public enum Reason {
    /** No {@code X-Unicom360-Signature} header, or an empty one. */
    MISSING_SIGNATURE("missing_signature"),
    /** No {@code X-Unicom360-Timestamp} header, or an empty one. */
    MISSING_TIMESTAMP("missing_timestamp"),
    /** The timestamp is not a whole number of seconds. */
    INVALID_TIMESTAMP("invalid_timestamp"),
    /** The signature is not {@code sha256=} followed by 64 lower-case hex digits. */
    MALFORMED_SIGNATURE("malformed_signature"),
    /** The timestamp is further from your clock than the tolerance (a replay, or clock skew). */
    TIMESTAMP_OUT_OF_TOLERANCE("timestamp_out_of_tolerance"),
    /** The signature does not match the body, the timestamp and the secret. */
    SIGNATURE_MISMATCH("signature_mismatch");

    private final String value;

    Reason(String value) {
      this.value = value;
    }

    /**
     * The reason as every Unicom360 SDK spells it, for example {@code signature_mismatch}.
     *
     * @return the reason string
     */
    public String value() {
      return value;
    }

    /**
     * Returns {@link #value()}.
     *
     * @return the reason string
     */
    @Override
    public String toString() {
      return value;
    }
  }

  /** Which check failed. */
  private final Reason reason;

  /**
   * Creates the exception.
   *
   * @param reason which check failed
   */
  public SignatureVerificationException(Reason reason) {
    super("Webhook signature verification failed: " + reason.value());
    this.reason = reason;
  }

  /**
   * Which check failed.
   *
   * @return the reason
   */
  public Reason reason() {
    return reason;
  }

  /**
   * Returns {@code signature_verification_error}.
   *
   * @return the type string
   */
  @Override
  public String type() {
    return "signature_verification_error";
  }
}
