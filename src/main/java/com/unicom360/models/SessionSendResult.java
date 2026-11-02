package com.unicom360.models;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.internal.Json;
import java.util.Map;

/**
 * The result of {@code client.whatsapp().sendSession(...)}.
 *
 * @param apiReference the request's reference: quote it to support
 * @param message the gateway's message ({@code successful})
 * @param messageId the message's id, for status lookups, {@code reply_to} and reactions
 * @param to the recipient
 * @param userReferenceId the reference sent with the message, or {@code null}
 * @param status {@code ACKNOWLEDGED}; other values pass through
 * @param category {@code SERVICE}; other values pass through
 * @param extra fields this version does not model
 * @param raw the JSON object this was parsed from
 */
public record SessionSendResult(
    String apiReference,
    String message,
    String messageId,
    String to,
    String userReferenceId,
    String status,
    String category,
    Map<String, JsonElement> extra,
    JsonObject raw) {

  /**
   * Copies the collections and JSON values, so the record is immutable.
   *
   * @param apiReference the request's reference: quote it to support
   * @param message the gateway's message ({@code successful})
   * @param messageId the message's id, for status lookups, {@code reply_to} and reactions
   * @param to the recipient
   * @param userReferenceId the reference sent with the message, or {@code null}
   * @param status {@code ACKNOWLEDGED}; other values pass through
   * @param category {@code SERVICE}; other values pass through
   * @param extra fields this version does not model
   * @param raw the JSON object this was parsed from
   */
  public SessionSendResult {
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
