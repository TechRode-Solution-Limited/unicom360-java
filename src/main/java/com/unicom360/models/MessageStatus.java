package com.unicom360.models;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.internal.Json;
import java.util.Map;

/**
 * One WhatsApp message's status. A {@code NOT_FOUND} entry has only {@code messageId} and {@code
 * status}.
 *
 * @param messageId the message's id
 * @param status {@code ACCEPTED}, {@code SENT}, {@code DELIVERED}, {@code READ}, {@code FAILED},
 *     {@code REJECTED} or {@code NOT_FOUND}; other values pass through
 * @param userReferenceId the message's reference, or {@code null}
 * @param to the recipient, or {@code null}
 * @param from the sender, or {@code null}
 * @param sentAt when it was sent (ISO 8601), or {@code null}
 * @param deliveredAt when it was delivered (ISO 8601), or {@code null}
 * @param readAt when it was read (ISO 8601), or {@code null}
 * @param error why it failed, or {@code null}
 * @param category the conversation category, or {@code null}
 * @param billable whether it was charged when sent, or {@code null}
 * @param refunded whether that charge was given back, or {@code null}
 * @param extra fields this version does not model
 * @param raw the JSON object this was parsed from
 */
public record MessageStatus(
    String messageId,
    String status,
    String userReferenceId,
    String to,
    String from,
    String sentAt,
    String deliveredAt,
    String readAt,
    MessageError error,
    String category,
    Boolean billable,
    Boolean refunded,
    Map<String, JsonElement> extra,
    JsonObject raw) {

  /**
   * Copies the collections and JSON values, so the record is immutable.
   *
   * @param messageId the message's id
   * @param status {@code ACCEPTED}, {@code SENT}, {@code DELIVERED}, {@code READ}, {@code FAILED},
   *     {@code REJECTED} or {@code NOT_FOUND}; other values pass through
   * @param userReferenceId the message's reference, or {@code null}
   * @param to the recipient, or {@code null}
   * @param from the sender, or {@code null}
   * @param sentAt when it was sent (ISO 8601), or {@code null}
   * @param deliveredAt when it was delivered (ISO 8601), or {@code null}
   * @param readAt when it was read (ISO 8601), or {@code null}
   * @param error why it failed, or {@code null}
   * @param category the conversation category, or {@code null}
   * @param billable whether it was charged when sent, or {@code null}
   * @param refunded whether that charge was given back, or {@code null}
   * @param extra fields this version does not model
   * @param raw the JSON object this was parsed from
   */
  public MessageStatus {
    extra = Json.copyExtra(extra);
    raw = Json.copy(raw);
  }

  /**
   * Every field this version does not model, so a field the gateway adds is readable today.
   *
   * @return a copy of the fields
   */
  @Override
  public Map<String, JsonElement> extra() {
    return Json.copyExtra(extra);
  }

  /**
   * The JSON object this was parsed from, unmodified.
   *
   * @return a copy of the object
   */
  @Override
  public JsonObject raw() {
    return raw.deepCopy();
  }
}
