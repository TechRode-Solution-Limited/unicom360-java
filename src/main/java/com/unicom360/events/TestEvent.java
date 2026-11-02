package com.unicom360.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.internal.Json;
import java.util.Map;

/**
 * A {@code webhook.test} event, sent by the portal's "Send test".
 *
 * @param event {@code webhook.test}
 * @param deliveryId the {@code X-Unicom360-Delivery} header, or {@code null}
 * @param createdAt when the event was created (ISO 8601), or {@code null}
 * @param data the test message, or {@code null} when the body had no {@code data} object
 * @param extra fields this version does not model
 * @param raw the JSON object this was parsed from
 */
public record TestEvent(
    String event,
    String deliveryId,
    String createdAt,
    TestEventData data,
    Map<String, JsonElement> extra,
    JsonObject raw)
    implements WebhookEvent {

  /**
   * Copies the collections and JSON values, so the record is immutable.
   *
   * @param event {@code webhook.test}
   * @param deliveryId the {@code X-Unicom360-Delivery} header, or {@code null}
   * @param createdAt when the event was created (ISO 8601), or {@code null}
   * @param data the test message, or {@code null} when the body had no {@code data} object
   * @param extra fields this version does not model
   * @param raw the JSON object this was parsed from
   */
  public TestEvent {
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
