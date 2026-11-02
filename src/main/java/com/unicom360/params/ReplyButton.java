package com.unicom360.params;

import com.google.gson.JsonObject;
import java.util.Objects;

/**
 * A reply button of an interactive message.
 *
 * @param id your id for the button
 * @param title the button's label
 */
public record ReplyButton(String id, String title) {

  /**
   * Requires an id and a title.
   *
   * @param id your id for the button
   * @param title the button's label
   */
  public ReplyButton {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(title, "title");
  }

  /**
   * A reply button.
   *
   * @param id your id for the button
   * @param title the button's label
   * @return the button
   */
  public static ReplyButton of(String id, String title) {
    return new ReplyButton(id, title);
  }

  JsonObject toJson() {
    JsonObject o = new JsonObject();
    o.addProperty("id", id);
    o.addProperty("title", title);
    return o;
  }
}
