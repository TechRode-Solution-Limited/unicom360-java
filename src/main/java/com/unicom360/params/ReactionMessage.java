package com.unicom360.params;

import com.google.gson.JsonObject;
import java.util.Objects;

/**
 * A reaction, built with {@link SessionMessage#reaction}. It has no {@code replyTo}: the gateway
 * does not allow one on a reaction. Every setter returns a copy.
 */
public final class ReactionMessage implements SessionMessage {
  private final String to;
  private final String messageId;
  private final String emoji;
  private final String from;
  private final String userReferenceId;
  private final String callbackUrl;

  ReactionMessage(String to, String messageId, String emoji) {
    this(to, messageId, emoji, null, null, null);
  }

  private ReactionMessage(
      String to,
      String messageId,
      String emoji,
      String from,
      String userReferenceId,
      String callbackUrl) {
    this.to = Objects.requireNonNull(to, "to");
    this.messageId = Objects.requireNonNull(messageId, "messageId");
    this.emoji = Objects.requireNonNull(emoji, "emoji");
    this.from = from;
    this.userReferenceId = userReferenceId;
    this.callbackUrl = callbackUrl;
  }

  @Override
  public String to() {
    return to;
  }

  /**
   * Returns {@code reaction}.
   *
   * @return the type
   */
  @Override
  public String type() {
    return "reaction";
  }

  /**
   * The message reacted to.
   *
   * @return the id
   */
  public String messageId() {
    return messageId;
  }

  /**
   * The emoji; {@code ""} removes your reaction.
   *
   * @return the emoji
   */
  public String emoji() {
    return emoji;
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
  public ReactionMessage from(String from) {
    return new ReactionMessage(to, messageId, emoji, from, userReferenceId, callbackUrl);
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
  public ReactionMessage userReferenceId(String userReferenceId) {
    return new ReactionMessage(to, messageId, emoji, from, userReferenceId, callbackUrl);
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
  public ReactionMessage callbackUrl(String callbackUrl) {
    return new ReactionMessage(to, messageId, emoji, from, userReferenceId, callbackUrl);
  }

  @Override
  public JsonObject toJson() {
    JsonObject reaction = new JsonObject();
    reaction.addProperty("message_id", messageId);
    reaction.addProperty("emoji", emoji);
    JsonObject o = new JsonObject();
    o.addProperty("to", to);
    o.addProperty("type", "reaction");
    o.add("reaction", reaction);
    if (from != null) {
      o.addProperty("from", from);
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
    return "ReactionMessage[]";
  }
}
