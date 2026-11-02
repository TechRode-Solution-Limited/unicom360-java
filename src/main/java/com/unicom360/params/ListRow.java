package com.unicom360.params;

import com.google.gson.JsonObject;
import java.util.Objects;

/**
 * A row of an interactive list.
 *
 * @param id your id for the row
 * @param title the row's title
 * @param description the row's description, or {@code null}
 */
public record ListRow(String id, String title, String description) {

  /**
   * Requires an id and a title.
   *
   * @param id your id for the row
   * @param title the row's title
   * @param description the row's description, or {@code null}
   */
  public ListRow {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(title, "title");
  }

  /**
   * A row without a description.
   *
   * @param id your id for the row
   * @param title the row's title
   * @return the row
   */
  public static ListRow of(String id, String title) {
    return new ListRow(id, title, null);
  }

  /**
   * A row with a description.
   *
   * @param id your id for the row
   * @param title the row's title
   * @param description the row's description, or {@code null}
   * @return the row
   */
  public static ListRow of(String id, String title, String description) {
    return new ListRow(id, title, description);
  }

  JsonObject toJson() {
    JsonObject o = new JsonObject();
    o.addProperty("id", id);
    o.addProperty("title", title);
    if (description != null) {
      o.addProperty("description", description);
    }
    return o;
  }
}
