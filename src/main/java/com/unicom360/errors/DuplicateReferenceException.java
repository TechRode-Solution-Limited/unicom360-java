package com.unicom360.errors;

import com.google.gson.JsonElement;

/**
 * The {@code user_reference_id} was already used for this recipient and sender ({@code
 * DUPLICATE_REFERENCE}, normalised from {@code duplicate user reference id} and {@code
 * DUPLICATE_MSG_REF}).
 *
 * <p><b>When {@link #attempts()} is greater than 1, an earlier attempt of the same call very
 * probably succeeded:</b> the retry was refused because that attempt had already used the
 * reference. Treat the message as sent and look it up rather than sending it again.
 */
public final class DuplicateReferenceException extends ApiException {
  private static final long serialVersionUID = 1L;

  /** The {@code message_id} the reference already went out as, or {@code null}. */
  private final String originalMessageId;

  /**
   * Creates the exception.
   *
   * @param message the gateway's message
   * @param details what the response said
   * @param attempts the HTTP attempts made, retries included
   */
  public DuplicateReferenceException(String message, ErrorDetails details, int attempts) {
    super(message, details, attempts);
    JsonElement data = details.data();
    JsonElement id =
        data != null && data.isJsonObject() ? data.getAsJsonObject().get("message_id") : null;
    this.originalMessageId =
        id != null && id.isJsonPrimitive() && id.getAsJsonPrimitive().isString()
            ? id.getAsString()
            : null;
  }

  /**
   * The {@code message_id} the reference already went out as (session sends only), or {@code null}
   * (other sends, or a rare race between two requests with the same reference).
   *
   * @return the original message id, or {@code null}
   */
  public String originalMessageId() {
    return originalMessageId;
  }
}
