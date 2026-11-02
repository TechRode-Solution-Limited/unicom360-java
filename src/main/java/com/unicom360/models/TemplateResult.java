package com.unicom360.models;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.internal.Json;
import java.util.Map;

/**
 * One recipient of a WhatsApp template send.
 *
 * @param messageId the message's id when it was queued (or the original after {@code
 *     DUPLICATE_MSG_REF}), else {@code null}
 * @param to the recipient as the request gave it (always a string)
 * @param userReferenceId the recipient's reference (always a string), or {@code null}
 * @param status {@code ACKNOWLEDGED} or {@code REJECTED}; other values pass through
 * @param reason why it was rejected (for example {@code INVALID_NUMBER}), {@code null} when
 *     accepted
 * @param category the template's category, for example {@code UTILITY}
 * @param extra fields this version does not model
 * @param raw the JSON object this was parsed from
 */
public record TemplateResult(
    String messageId,
    String to,
    String userReferenceId,
    String status,
    String reason,
    String category,
    Map<String, JsonElement> extra,
    JsonObject raw) {

  /**
   * Copies the collections and JSON values, so the record is immutable.
   *
   * @param messageId the message's id when it was queued (or the original after {@code
   *     DUPLICATE_MSG_REF}), else {@code null}
   * @param to the recipient as the request gave it (always a string)
   * @param userReferenceId the recipient's reference (always a string), or {@code null}
   * @param status {@code ACKNOWLEDGED} or {@code REJECTED}; other values pass through
   * @param reason why it was rejected (for example {@code INVALID_NUMBER}), {@code null} when
   *     accepted
   * @param category the template's category, for example {@code UTILITY}
   * @param extra fields this version does not model
   * @param raw the JSON object this was parsed from
   */
  public TemplateResult {
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
