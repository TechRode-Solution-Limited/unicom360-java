package com.unicom360.models;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.internal.Json;
import java.util.Map;

/**
 * The result of {@code client.health()}.
 *
 * @param status {@code ok} when the gateway is up
 * @param timestamp the gateway's time (ISO 8601)
 * @param version the gateway's version
 * @param environment the environment, for example {@code production}
 * @param extra fields this version does not model
 * @param raw the JSON object this was parsed from
 */
public record Health(
    String status,
    String timestamp,
    String version,
    String environment,
    Map<String, JsonElement> extra,
    JsonObject raw) {

  /**
   * Copies the collections and JSON values, so the record is immutable.
   *
   * @param status {@code ok} when the gateway is up
   * @param timestamp the gateway's time (ISO 8601)
   * @param version the gateway's version
   * @param environment the environment, for example {@code production}
   * @param extra fields this version does not model
   * @param raw the JSON object this was parsed from
   */
  public Health {
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
