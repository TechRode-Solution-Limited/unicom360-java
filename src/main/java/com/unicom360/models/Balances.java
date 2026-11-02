package com.unicom360.models;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.internal.Json;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The result of {@code client.balance().all()}: every bundle's balance.
 *
 * @param message the gateway's message
 * @param balances the units left per bundle type (for example {@code SMS}), in the response's order
 * @param clientId your client id
 * @param clientName your client name
 * @param extra fields this version does not model
 * @param raw the JSON object this was parsed from
 */
public record Balances(
    String message,
    Map<String, BigDecimal> balances,
    String clientId,
    String clientName,
    Map<String, JsonElement> extra,
    JsonObject raw) {

  /**
   * Copies the collections and JSON values, so the record is immutable.
   *
   * @param message the gateway's message
   * @param balances the units left per bundle type (for example {@code SMS}), in the response's
   *     order
   * @param clientId your client id
   * @param clientName your client name
   * @param extra fields this version does not model
   * @param raw the JSON object this was parsed from
   */
  public Balances {
    balances =
        balances == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(balances));
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
