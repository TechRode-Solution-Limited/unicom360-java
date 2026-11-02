package com.unicom360.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.internal.Json;
import java.util.Map;

/**
 * The {@code data} of a {@code message.received} event: a customer's message.
 *
 * <p>A media caption arrives in {@code text}. {@code payload} is set only for location, contacts
 * ({@code {"contacts": [...]}}), reaction, sticker and unsupported messages. An interactive or
 * button reply has the chosen title in {@code text}, a {@code null} payload, and {@code replyTo}
 * set to the {@code wamid.} id of your interactive message; the button or row id is not passed on.
 *
 * @param messageId Meta's id of the message ({@code wamid.} followed by Meta's id): pass it as
 *     {@code reply_to} or a reaction's {@code message_id}
 * @param channel {@code whatsapp}
 * @param from the customer's number
 * @param to your number
 * @param type the message type, for example {@code text}, {@code image} or {@code interactive};
 *     other values pass through
 * @param text the text, a caption, or the chosen title of a reply, or {@code null}
 * @param media the media, or {@code null}
 * @param payload Meta's object for location, contacts, reaction, sticker and unsupported messages,
 *     or {@code null}
 * @param replyTo the {@code wamid.} id of the message this replies to, or {@code null}
 * @param timestamp when it was sent (ISO 8601)
 * @param windowExpiresAt when the 24-hour service window closes (ISO 8601)
 * @param extra fields this version does not model
 * @param raw the JSON object this was parsed from
 */
public record InboundMessage(
    String messageId,
    String channel,
    String from,
    String to,
    String type,
    String text,
    InboundMedia media,
    JsonObject payload,
    String replyTo,
    String timestamp,
    String windowExpiresAt,
    Map<String, JsonElement> extra,
    JsonObject raw) {

  /**
   * Copies the collections and JSON values, so the record is immutable.
   *
   * @param messageId Meta's id of the message ({@code wamid.} followed by Meta's id): pass it as
   *     {@code reply_to} or a reaction's {@code message_id}
   * @param channel {@code whatsapp}
   * @param from the customer's number
   * @param to your number
   * @param type the message type, for example {@code text}, {@code image} or {@code interactive};
   *     other values pass through
   * @param text the text, a caption, or the chosen title of a reply, or {@code null}
   * @param media the media, or {@code null}
   * @param payload Meta's object for location, contacts, reaction, sticker and unsupported
   *     messages, or {@code null}
   * @param replyTo the {@code wamid.} id of the message this replies to, or {@code null}
   * @param timestamp when it was sent (ISO 8601)
   * @param windowExpiresAt when the 24-hour service window closes (ISO 8601)
   * @param extra fields this version does not model
   * @param raw the JSON object this was parsed from
   */
  public InboundMessage {
    payload = payload == null ? null : payload.deepCopy();
    extra = Json.copyExtra(extra);
    raw = Json.copy(raw);
  }

  /**
   * Meta's object for location, contacts, reaction, sticker and unsupported messages, as a copy.
   *
   * @return a copy, or {@code null}
   */
  @Override
  public JsonObject payload() {
    return payload == null ? null : payload.deepCopy();
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
