package com.unicom360.params;

import com.google.gson.JsonObject;
import java.util.Objects;

/**
 * A template recipient's button parameter.
 *
 * @param index the button's position in the template
 * @param type {@code url} or {@code quick_reply}
 * @param value the URL suffix for a {@code {{1}}} in a URL button, or a quick reply's payload
 */
public record TemplateButton(int index, String type, String value) {

  /**
   * Requires a type and a value.
   *
   * @param index the button's position in the template
   * @param type {@code url} or {@code quick_reply}
   * @param value the URL suffix for a {@code {{1}}} in a URL button, or a quick reply's payload
   */
  public TemplateButton {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(value, "value");
  }

  /**
   * A URL button's dynamic part.
   *
   * @param index the button's position in the template
   * @param value the value for the {@code {{1}}} in the button's URL
   * @return the button
   */
  public static TemplateButton url(int index, String value) {
    return new TemplateButton(index, "url", value);
  }

  /**
   * A quick reply button's payload.
   *
   * @param index the button's position in the template
   * @param value the payload
   * @return the button
   */
  public static TemplateButton quickReply(int index, String value) {
    return new TemplateButton(index, "quick_reply", value);
  }

  /**
   * The JSON this sends: {@code {"index", "type", "value"}}.
   *
   * @return a new JSON object
   */
  public JsonObject toJson() {
    JsonObject o = new JsonObject();
    o.addProperty("index", index);
    o.addProperty("type", type);
    o.addProperty("value", value);
    return o;
  }
}
