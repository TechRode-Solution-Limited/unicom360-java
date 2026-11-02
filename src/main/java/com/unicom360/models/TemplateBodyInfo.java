package com.unicom360.models;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.internal.Json;
import java.util.List;
import java.util.Map;

/**
 * A template's body.
 *
 * @param text the body text with its {@code {{n}}} placeholders, or {@code null}
 * @param variables the body's variables
 * @param extra fields this version does not model
 * @param raw the JSON object this was parsed from
 */
public record TemplateBodyInfo(
    String text, List<String> variables, Map<String, JsonElement> extra, JsonObject raw) {

  /**
   * Copies the collections and JSON values, so the record is immutable.
   *
   * @param text the body text with its {@code {{n}}} placeholders, or {@code null}
   * @param variables the body's variables
   * @param extra fields this version does not model
   * @param raw the JSON object this was parsed from
   */
  public TemplateBodyInfo {
    variables = variables == null ? List.of() : List.copyOf(variables);
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
