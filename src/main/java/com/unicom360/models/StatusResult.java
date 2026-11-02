package com.unicom360.models;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.internal.Json;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The result of {@code client.whatsapp().status(...)}: one entry per distinct id.
 *
 * @param apiReference the request's reference: quote it to support
 * @param message the gateway's message ({@code successful})
 * @param results the statuses
 * @param extra fields this version does not model
 * @param raw the JSON object this was parsed from
 */
public record StatusResult(
    String apiReference,
    String message,
    List<MessageStatus> results,
    Map<String, JsonElement> extra,
    JsonObject raw) {

  /**
   * Copies the collections and JSON values, so the record is immutable.
   *
   * @param apiReference the request's reference: quote it to support
   * @param message the gateway's message ({@code successful})
   * @param results the statuses
   * @param extra fields this version does not model
   * @param raw the JSON object this was parsed from
   */
  public StatusResult {
    results = results == null ? List.of() : List.copyOf(results);
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

  /**
   * The status of {@code messageId} (exact match).
   *
   * @param messageId the message's id
   * @return the status, or empty when the result has no entry for it
   */
  public Optional<MessageStatus> byId(String messageId) {
    return results.stream()
        .filter(r -> messageId != null && messageId.equals(r.messageId()))
        .findFirst();
  }
}
