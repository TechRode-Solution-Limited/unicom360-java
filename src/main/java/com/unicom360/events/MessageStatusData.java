package com.unicom360.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.internal.Json;
import com.unicom360.models.MessageError;
import java.util.Map;

/**
 * The {@code data} of a {@code message.status} event.
 *
 * @param messageId the {@code message_id} the send returned
 * @param userReferenceId the message's reference, or {@code null}
 * @param channel {@code whatsapp}
 * @param to the recipient
 * @param from your sender
 * @param status {@code sent}, {@code delivered}, {@code read} or {@code failed}; other values pass
 *     through
 * @param timestamp when the status changed (ISO 8601)
 * @param error why it failed, or {@code null}
 * @param category the conversation category, or {@code null}
 * @param billable whether it was charged, or {@code null}
 * @param extra fields this version does not model
 * @param raw the JSON object this was parsed from
 */
public record MessageStatusData(
    String messageId,
    String userReferenceId,
    String channel,
    String to,
    String from,
    String status,
    String timestamp,
    MessageError error,
    String category,
    Boolean billable,
    Map<String, JsonElement> extra,
    JsonObject raw) {

  /**
   * Copies the collections and JSON values, so the record is immutable.
   *
   * @param messageId the {@code message_id} the send returned
   * @param userReferenceId the message's reference, or {@code null}
   * @param channel {@code whatsapp}
   * @param to the recipient
   * @param from your sender
   * @param status {@code sent}, {@code delivered}, {@code read} or {@code failed}; other values
   *     pass through
   * @param timestamp when the status changed (ISO 8601)
   * @param error why it failed, or {@code null}
   * @param category the conversation category, or {@code null}
   * @param billable whether it was charged, or {@code null}
   * @param extra fields this version does not model
   * @param raw the JSON object this was parsed from
   */
  public MessageStatusData {
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
