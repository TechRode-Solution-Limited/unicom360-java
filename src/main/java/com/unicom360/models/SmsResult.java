package com.unicom360.models;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.internal.Json;
import java.util.Map;

/**
 * One recipient of an SMS send.
 *
 * @param messageId the message's id: keep it to match the delivery report
 * @param status {@code ACKNOWLEDGED} (accepted) or {@code Invalid mobile number}; other values pass
 *     through
 * @param mobileNumber the number as the gateway normalised it
 * @param extra fields this version does not model
 * @param raw the JSON object this was parsed from
 */
public record SmsResult(
    String messageId,
    String status,
    String mobileNumber,
    Map<String, JsonElement> extra,
    JsonObject raw) {

  /**
   * Copies the collections and JSON values, so the record is immutable.
   *
   * @param messageId the message's id: keep it to match the delivery report
   * @param status {@code ACKNOWLEDGED} (accepted) or {@code Invalid mobile number}; other values
   *     pass through
   * @param mobileNumber the number as the gateway normalised it
   * @param extra fields this version does not model
   * @param raw the JSON object this was parsed from
   */
  public SmsResult {
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
