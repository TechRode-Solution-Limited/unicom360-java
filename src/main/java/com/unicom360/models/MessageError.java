package com.unicom360.models;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.internal.Json;
import java.util.Map;

/**
 * Why a WhatsApp message failed.
 *
 * @param code Meta's error code, or {@code null}
 * @param message the error's description
 * @param extra fields this version does not model
 * @param raw the JSON object this was parsed from
 */
public record MessageError(
    Integer code, String message, Map<String, JsonElement> extra, JsonObject raw) {

  /**
   * Copies the collections and JSON values, so the record is immutable.
   *
   * @param code Meta's error code, or {@code null}
   * @param message the error's description
   * @param extra fields this version does not model
   * @param raw the JSON object this was parsed from
   */
  public MessageError {
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
