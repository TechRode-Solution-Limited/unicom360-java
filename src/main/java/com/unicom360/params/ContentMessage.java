package com.unicom360.params;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;

/**
 * A session message with content: text, media, an interactive message, a location or contact cards.
 * Built with the {@link SessionMessage} factories. Every setter returns a copy.
 */
public final class ContentMessage implements SessionMessage {
  private final String to;
  private final String type;
  private final String contentKey;
  private final JsonElement content;
  private final String from;
  private final String replyTo;
  private final String userReferenceId;
  private final String callbackUrl;

  ContentMessage(String to, String type, String contentKey, JsonElement content) {
    this(to, type, contentKey, content, null, null, null, null);
  }

  private ContentMessage(
      String to,
      String type,
      String contentKey,
      JsonElement content,
      String from,
      String replyTo,
      String userReferenceId,
      String callbackUrl) {
    this.to = Objects.requireNonNull(to, "to");
    this.type = type;
    this.contentKey = contentKey;
    this.content = content;
    this.from = from;
    this.replyTo = replyTo;
    this.userReferenceId = userReferenceId;
    this.callbackUrl = callbackUrl;
  }

  @Override
  public String to() {
    return to;
  }

  @Override
  public String type() {
    return type;
  }

  @Override
  public String from() {
    return from;
  }

  /**
   * A copy with your WhatsApp sender.
   *
   * @param from one of your approved senders, or {@code null} for none
   * @return the copy
   */
  public ContentMessage from(String from) {
    return new ContentMessage(
        to, type, contentKey, content, from, replyTo, userReferenceId, callbackUrl);
  }

  /**
   * The message this quotes, or {@code null}.
   *
   * @return the id, or {@code null}
   */
  public String replyTo() {
    return replyTo;
  }

  /**
   * A copy that quotes a message.
   *
   * @param replyTo a {@code message_id} a send returned, or the {@code wamid.} id of an inbound
   *     message, or {@code null} for none
   * @return the copy
   */
  public ContentMessage replyTo(String replyTo) {
    return new ContentMessage(
        to, type, contentKey, content, from, replyTo, userReferenceId, callbackUrl);
  }

  @Override
  public String userReferenceId() {
    return userReferenceId;
  }

  /**
   * A copy with your reference, which makes the send safe to retry.
   *
   * @param userReferenceId the reference, or {@code null} for none
   * @return the copy
   */
  public ContentMessage userReferenceId(String userReferenceId) {
    return new ContentMessage(
        to, type, contentKey, content, from, replyTo, userReferenceId, callbackUrl);
  }

  @Override
  public String callbackUrl() {
    return callbackUrl;
  }

  /**
   * A copy with a status callback URL.
   *
   * @param callbackUrl an https URL, or {@code null} for none
   * @return the copy
   */
  public ContentMessage callbackUrl(String callbackUrl) {
    return new ContentMessage(
        to, type, contentKey, content, from, replyTo, userReferenceId, callbackUrl);
  }

  @Override
  public JsonObject toJson() {
    JsonObject o = new JsonObject();
    o.addProperty("to", to);
    o.addProperty("type", type);
    o.add(contentKey, content.deepCopy());
    if (from != null) {
      o.addProperty("from", from);
    }
    if (replyTo != null) {
      o.addProperty("reply_to", replyTo);
    }
    if (userReferenceId != null) {
      o.addProperty("user_reference_id", userReferenceId);
    }
    if (callbackUrl != null) {
      o.addProperty("callback_url", callbackUrl);
    }
    return o;
  }

  @Override
  public String toString() {
    return "ContentMessage[type=" + type + "]";
  }
}
