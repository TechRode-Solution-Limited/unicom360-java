package com.unicom360.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.internal.Json;
import java.util.Map;

/**
 * A delivery report posted to an SMS {@code callback_url}.
 *
 * <p><b>These reports are unsigned and never retried,</b> so anyone can post one: treat them as
 * hints.
 *
 * @param messageId the {@code message_id} the send returned
 * @param status {@code SENT}, {@code DELIVERED}, {@code FAILED} or {@code UNKNOWN_ERROR}; other
 *     values pass through
 * @param mobileNumber the recipient
 * @param extra fields this version does not model
 * @param raw the JSON object this was parsed from
 */
public record SmsDeliveryReport(
    String messageId,
    String status,
    String mobileNumber,
    Map<String, JsonElement> extra,
    JsonObject raw) {

  /**
   * Copies the collections and JSON values, so the record is immutable.
   *
   * @param messageId the {@code message_id} the send returned
   * @param status {@code SENT}, {@code DELIVERED}, {@code FAILED} or {@code UNKNOWN_ERROR}; other
   *     values pass through
   * @param mobileNumber the recipient
   * @param extra fields this version does not model
   * @param raw the JSON object this was parsed from
   */
  public SmsDeliveryReport {
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
