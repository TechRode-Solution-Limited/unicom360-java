package com.unicom360.params;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;

/**
 * A section of an interactive list. The title is required by the gateway when there is more than
 * one section.
 *
 * @param title the section's title, or {@code null}
 * @param rows the section's rows
 */
public record ListSection(String title, List<ListRow> rows) {

  /**
   * Copies the rows.
   *
   * @param title the section's title, or {@code null}
   * @param rows the section's rows
   */
  public ListSection {
    rows = List.copyOf(rows);
  }

  /**
   * A section without a title.
   *
   * @param rows the section's rows
   * @return the section
   */
  public static ListSection of(List<ListRow> rows) {
    return new ListSection(null, rows);
  }

  /**
   * A section with a title.
   *
   * @param title the section's title, or {@code null}
   * @param rows the section's rows
   * @return the section
   */
  public static ListSection of(String title, List<ListRow> rows) {
    return new ListSection(title, rows);
  }

  JsonObject toJson() {
    JsonObject o = new JsonObject();
    if (title != null) {
      o.addProperty("title", title);
    }
    JsonArray array = new JsonArray();
    rows.forEach(row -> array.add(row.toJson()));
    o.add("rows", array);
    return o;
  }
}
