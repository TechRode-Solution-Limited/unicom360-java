package com.unicom360.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.internal.Json;
import java.util.Map;

/**
 * An event this version does not know. Parsing it never fails: read {@link #data()} or {@link
 * #raw()}.
 *
 * @param event the event's name
 * @param deliveryId the {@code X-Unicom360-Delivery} header, or {@code null}
 * @param createdAt when the event was created (ISO 8601), or {@code null}
 * @param data the body's {@code data}, or {@code null}
 * @param extra fields this version does not model
 * @param raw the JSON object this was parsed from
 */
public record UnknownEvent(
    String event,
    String deliveryId,
    String createdAt,
    JsonElement data,
    Map<String, JsonElement> extra,
    JsonObject raw)
    implements WebhookEvent {

  /**
   * Copies the collections and JSON values, so the record is immutable.
   *
   * @param event the event's name
   * @param deliveryId the {@code X-Unicom360-Delivery} header, or {@code null}
   * @param createdAt when the event was created (ISO 8601), or {@code null}
   * @param data the body's {@code data}, or {@code null}
   * @param extra fields this version does not model
   * @param raw the JSON object this was parsed from
   */
  public UnknownEvent {
    data = data == null ? null : data.deepCopy();
    extra = Json.copyExtra(extra);
    raw = Json.copy(raw);
  }

  /**
   * The body's {@code data} as a copy.
   *
   * @return a copy, or {@code null}
   */
  @Override
  public JsonElement data() {
    return data == null ? null : data.deepCopy();
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
