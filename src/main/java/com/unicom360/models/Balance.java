package com.unicom360.models;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.internal.Json;
import java.math.BigDecimal;
import java.util.Map;

/**
 * The result of {@code client.balance().get(...)}: one bundle's balance.
 *
 * @param message the gateway's message
 * @param balance the units left, exactly as the gateway sent them, or {@code null}
 * @param bundleType the bundle, for example {@code SMS}
 * @param clientId your client id
 * @param clientName your client name
 * @param extra fields this version does not model
 * @param raw the JSON object this was parsed from
 */
public record Balance(
    String message,
    BigDecimal balance,
    String bundleType,
    String clientId,
    String clientName,
    Map<String, JsonElement> extra,
    JsonObject raw) {

  /**
   * Copies the collections and JSON values, so the record is immutable.
   *
   * @param message the gateway's message
   * @param balance the units left, exactly as the gateway sent them, or {@code null}
   * @param bundleType the bundle, for example {@code SMS}
   * @param clientId your client id
   * @param clientName your client name
   * @param extra fields this version does not model
   * @param raw the JSON object this was parsed from
   */
  public Balance {
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
