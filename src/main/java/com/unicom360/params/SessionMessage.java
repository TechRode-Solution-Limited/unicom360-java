package com.unicom360.params;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.List;
import java.util.Objects;

/**
 * A WhatsApp session message, for {@code client.whatsapp().sendSession(...)}: one factory per
 * content type, valid inside the 24-hour customer service window.
 *
 * <p>Messages are immutable. The common optional fields are immutable setters that return a copy:
 *
 * <pre>{@code
 * client.whatsapp().sendSession(
 *     SessionMessage.text("260971234567", "Your order is ready.")
 *         .from("260960123456")
 *         .replyTo(inboundMessageId)
 *         .userReferenceId("s-1")
 *         .callbackUrl("https://example.com/webhooks/unicom360/whatsapp"));
 * }</pre>
 *
 * <p>{@code replyTo} and a reaction's {@code messageId} take either a {@code message_id} a send
 * returned or the {@code wamid.} id of an inbound message, exactly as a {@code message.received}
 * webhook delivers it. A reaction has no {@code replyTo}, so {@link #reaction} returns a {@link
 * ReactionMessage} and every other factory a {@link ContentMessage}.
 */
public sealed interface SessionMessage permits ContentMessage, ReactionMessage {

  /**
   * A text message.
   *
   * @param to the customer's number
   * @param text 1 to 4,096 characters
   * @return the message
   */
  static ContentMessage text(String to, String text) {
    Objects.requireNonNull(text, "text");
    return new ContentMessage(to, "text", "text", new JsonPrimitive(text));
  }

  /**
   * An image.
   *
   * @param to the customer's number
   * @param url an https URL Meta can fetch
   * @return the message
   */
  static ContentMessage image(String to, String url) {
    return image(to, url, null);
  }

  /**
   * An image with a caption.
   *
   * @param to the customer's number
   * @param url an https URL Meta can fetch
   * @param caption the caption, or {@code null}
   * @return the message
   */
  static ContentMessage image(String to, String url, String caption) {
    return new ContentMessage(to, "image", "media", media(url, caption, null));
  }

  /**
   * A video.
   *
   * @param to the customer's number
   * @param url an https URL Meta can fetch
   * @return the message
   */
  static ContentMessage video(String to, String url) {
    return video(to, url, null);
  }

  /**
   * A video with a caption.
   *
   * @param to the customer's number
   * @param url an https URL Meta can fetch
   * @param caption the caption, or {@code null}
   * @return the message
   */
  static ContentMessage video(String to, String url, String caption) {
    return new ContentMessage(to, "video", "media", media(url, caption, null));
  }

  /**
   * A document.
   *
   * @param to the customer's number
   * @param url an https URL Meta can fetch
   * @return the message
   */
  static ContentMessage document(String to, String url) {
    return document(to, url, null, null);
  }

  /**
   * A document with a caption.
   *
   * @param to the customer's number
   * @param url an https URL Meta can fetch
   * @param caption the caption, or {@code null}
   * @return the message
   */
  static ContentMessage document(String to, String url, String caption) {
    return document(to, url, caption, null);
  }

  /**
   * A document with a caption and a file name.
   *
   * @param to the customer's number
   * @param url an https URL Meta can fetch
   * @param caption the caption, or {@code null}
   * @param filename the name it is shown with, or {@code null}
   * @return the message
   */
  static ContentMessage document(String to, String url, String caption, String filename) {
    return new ContentMessage(to, "document", "media", media(url, caption, filename));
  }

  /**
   * An audio message.
   *
   * @param to the customer's number
   * @param url an https URL Meta can fetch
   * @return the message
   */
  static ContentMessage audio(String to, String url) {
    return new ContentMessage(to, "audio", "media", media(url, null, null));
  }

  /**
   * A sticker.
   *
   * @param to the customer's number
   * @param url an https URL Meta can fetch
   * @return the message
   */
  static ContentMessage sticker(String to, String url) {
    return new ContentMessage(to, "sticker", "media", media(url, null, null));
  }

  /**
   * Up to three reply buttons.
   *
   * @param to the customer's number
   * @param body the message's text
   * @param buttons the buttons
   * @return the message
   */
  static ContentMessage buttons(String to, String body, List<ReplyButton> buttons) {
    return buttons(to, body, buttons, null, null);
  }

  /**
   * Up to three reply buttons, with a header and a footer.
   *
   * @param to the customer's number
   * @param body the message's text
   * @param buttons the buttons
   * @param header the header text, or {@code null}
   * @param footer the footer text, or {@code null}
   * @return the message
   */
  static ContentMessage buttons(
      String to, String body, List<ReplyButton> buttons, String header, String footer) {
    Objects.requireNonNull(body, "body");
    Objects.requireNonNull(buttons, "buttons");
    JsonObject o = new JsonObject();
    o.addProperty("type", "button");
    o.addProperty("body", body);
    JsonArray array = new JsonArray();
    buttons.forEach(button -> array.add(button.toJson()));
    o.add("buttons", array);
    if (header != null) {
      o.addProperty("header", header);
    }
    if (footer != null) {
      o.addProperty("footer", footer);
    }
    return new ContentMessage(to, "interactive", "interactive", o);
  }

  /**
   * A list.
   *
   * @param to the customer's number
   * @param body the message's text
   * @param button the text of the button that opens the list
   * @param sections the sections
   * @return the message
   */
  static ContentMessage list(String to, String body, String button, List<ListSection> sections) {
    return list(to, body, button, sections, null, null);
  }

  /**
   * A list, with a header and a footer.
   *
   * @param to the customer's number
   * @param body the message's text
   * @param button the text of the button that opens the list
   * @param sections the sections
   * @param header the header text, or {@code null}
   * @param footer the footer text, or {@code null}
   * @return the message
   */
  static ContentMessage list(
      String to,
      String body,
      String button,
      List<ListSection> sections,
      String header,
      String footer) {
    Objects.requireNonNull(body, "body");
    Objects.requireNonNull(button, "button");
    Objects.requireNonNull(sections, "sections");
    JsonObject o = new JsonObject();
    o.addProperty("type", "list");
    o.addProperty("body", body);
    o.addProperty("button", button);
    JsonArray array = new JsonArray();
    sections.forEach(section -> array.add(section.toJson()));
    o.add("sections", array);
    if (header != null) {
      o.addProperty("header", header);
    }
    if (footer != null) {
      o.addProperty("footer", footer);
    }
    return new ContentMessage(to, "interactive", "interactive", o);
  }

  /**
   * A location pin.
   *
   * @param to the customer's number
   * @param latitude the latitude
   * @param longitude the longitude
   * @return the message
   */
  static ContentMessage location(String to, double latitude, double longitude) {
    return location(to, latitude, longitude, null, null);
  }

  /**
   * A location pin with a name and an address.
   *
   * @param to the customer's number
   * @param latitude the latitude
   * @param longitude the longitude
   * @param name the place's name, or {@code null}
   * @param address the place's address, or {@code null}
   * @return the message
   */
  static ContentMessage location(
      String to, double latitude, double longitude, String name, String address) {
    JsonObject o = new JsonObject();
    o.addProperty("latitude", latitude);
    o.addProperty("longitude", longitude);
    if (name != null) {
      o.addProperty("name", name);
    }
    if (address != null) {
      o.addProperty("address", address);
    }
    return new ContentMessage(to, "location", "location", o);
  }

  /**
   * Contact cards.
   *
   * @param to the customer's number
   * @param contacts 1 to 10 cards
   * @return the message
   */
  static ContentMessage contacts(String to, List<ContactCard> contacts) {
    Objects.requireNonNull(contacts, "contacts");
    JsonArray array = new JsonArray();
    contacts.forEach(contact -> array.add(contact.toJson()));
    return new ContentMessage(to, "contacts", "contacts", array);
  }

  /**
   * A reaction to a message.
   *
   * @param to the customer's number
   * @param messageId the {@code message_id} a send returned, or the {@code wamid.} id of an inbound
   *     message
   * @param emoji one emoji, or {@code ""} to remove your reaction
   * @return the message
   */
  static ReactionMessage reaction(String to, String messageId, String emoji) {
    return new ReactionMessage(to, messageId, emoji);
  }

  /**
   * The customer's number.
   *
   * @return the number
   */
  String to();

  /**
   * The message type: {@code text}, {@code image}, {@code interactive}, {@code reaction}, ...
   *
   * @return the type
   */
  String type();

  /**
   * Your WhatsApp sender, or {@code null} (optional with exactly one approved sender).
   *
   * @return the sender, or {@code null}
   */
  String from();

  /**
   * Your reference, or {@code null}. With one, a send is safe to retry.
   *
   * @return the reference, or {@code null}
   */
  String userReferenceId();

  /**
   * Where status callbacks are posted, or {@code null}.
   *
   * @return the URL, or {@code null}
   */
  String callbackUrl();

  /**
   * The JSON this sends, with the API's field names.
   *
   * @return a new JSON object
   */
  JsonObject toJson();

  private static JsonObject media(String url, String caption, String filename) {
    Objects.requireNonNull(url, "url");
    JsonObject o = new JsonObject();
    o.addProperty("url", url);
    if (caption != null) {
      o.addProperty("caption", caption);
    }
    if (filename != null) {
      o.addProperty("filename", filename);
    }
    return o;
  }
}
