package com.unicom360.params;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Objects;

/**
 * A template recipient's header parameter: a text value, or a media header (image, video or
 * document).
 */
public sealed interface TemplateHeader permits TemplateHeader.Text, TemplateHeader.Media {

  /**
   * A text header's variable, sent as a JSON string.
   *
   * @param value the text
   * @return the header
   */
  static Text text(String value) {
    return new Text(value);
  }

  /**
   * An image header.
   *
   * @param url an https URL Meta can fetch
   * @return the header
   */
  static Media image(String url) {
    return new Media("image", url, null);
  }

  /**
   * A video header.
   *
   * @param url an https URL Meta can fetch
   * @return the header
   */
  static Media video(String url) {
    return new Media("video", url, null);
  }

  /**
   * A document header.
   *
   * @param url an https URL Meta can fetch
   * @return the header
   */
  static Media document(String url) {
    return new Media("document", url, null);
  }

  /**
   * A document header shown with a file name.
   *
   * @param url an https URL Meta can fetch
   * @param filename the name the document is shown with, or {@code null}
   * @return the header
   */
  static Media document(String url, String filename) {
    return new Media("document", url, filename);
  }

  /**
   * The JSON this sends: a string, or {@code {"type", "url", "filename"?}}.
   *
   * @return the JSON value
   */
  JsonElement toJson();

  /**
   * A text header's variable.
   *
   * @param value the text
   */
  record Text(String value) implements TemplateHeader {
    /**
     * Requires a value.
     *
     * @param value the text
     */
    public Text {
      Objects.requireNonNull(value, "value");
    }

    @Override
    public JsonElement toJson() {
      return new JsonPrimitive(value);
    }
  }

  /**
   * A media header.
   *
   * @param type {@code image}, {@code video} or {@code document}
   * @param url the media's URL
   * @param filename a document's file name, or {@code null}
   */
  record Media(String type, String url, String filename) implements TemplateHeader {
    /**
     * Requires a type and a URL.
     *
     * @param type {@code image}, {@code video} or {@code document}
     * @param url the media's URL
     * @param filename a document's file name, or {@code null}
     */
    public Media {
      Objects.requireNonNull(type, "type");
      Objects.requireNonNull(url, "url");
    }

    @Override
    public JsonElement toJson() {
      JsonObject o = new JsonObject();
      o.addProperty("type", type);
      o.addProperty("url", url);
      if (filename != null) {
        o.addProperty("filename", filename);
      }
      return o;
    }
  }
}
