package com.unicom360.models;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.internal.Json;
import java.util.Map;

/**
 * A template's button.
 *
 * @param index the button's position
 * @param type {@code url}, {@code quick_reply}, {@code phone_number} or {@code otp}; other values
 *     pass through
 * @param text the button's label, or {@code null}
 * @param url a URL button's URL (a {@code {{1}}} in it is filled per recipient), or {@code null}
 * @param otpType an OTP button's type, or {@code null}
 * @param extra fields this version does not model
 * @param raw the JSON object this was parsed from
 */
public record TemplateButtonInfo(
    int index,
    String type,
    String text,
    String url,
    String otpType,
    Map<String, JsonElement> extra,
    JsonObject raw) {

  /**
   * Copies the collections and JSON values, so the record is immutable.
   *
   * @param index the button's position
   * @param type {@code url}, {@code quick_reply}, {@code phone_number} or {@code otp}; other values
   *     pass through
   * @param text the button's label, or {@code null}
   * @param url a URL button's URL (a {@code {{1}}} in it is filled per recipient), or {@code null}
   * @param otpType an OTP button's type, or {@code null}
   * @param extra fields this version does not model
   * @param raw the JSON object this was parsed from
   */
  public TemplateButtonInfo {
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
