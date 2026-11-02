package com.unicom360.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.internal.Json;
import java.util.Map;

/**
 * The {@code data} of a {@code webhook.test} event (the portal's "Send test").
 *
 * @param message the test message
 * @param extra fields this version does not model
 * @param raw the JSON object this was parsed from
 */
public record TestEventData(String message, Map<String, JsonElement> extra, JsonObject raw) {

  /**
   * Copies the collections and JSON values, so the record is immutable.
   *
   * @param message the test message
   * @param extra fields this version does not model
   * @param raw the JSON object this was parsed from
   */
  public TestEventData {
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
