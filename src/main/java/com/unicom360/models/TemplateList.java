package com.unicom360.models;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.internal.Json;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The result of {@code client.whatsapp().templates()}: the approved templates.
 *
 * @param apiReference the request's reference: quote it to support
 * @param message the gateway's message ({@code successful})
 * @param templates the templates
 * @param extra fields this version does not model
 * @param raw the JSON object this was parsed from
 */
public record TemplateList(
    String apiReference,
    String message,
    List<TemplateInfo> templates,
    Map<String, JsonElement> extra,
    JsonObject raw) {

  /**
   * Copies the collections and JSON values, so the record is immutable.
   *
   * @param apiReference the request's reference: quote it to support
   * @param message the gateway's message ({@code successful})
   * @param templates the templates
   * @param extra fields this version does not model
   * @param raw the JSON object this was parsed from
   */
  public TemplateList {
    templates = templates == null ? List.of() : List.copyOf(templates);
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

  /**
   * The first template named {@code name} (exact match), in any language.
   *
   * @param name the template's name
   * @return the template, or empty
   */
  public Optional<TemplateInfo> byName(String name) {
    return byName(name, null);
  }

  /**
   * The template named {@code name} in {@code language} (exact matches).
   *
   * @param name the template's name
   * @param language the language code, or {@code null} for any
   * @return the template, or empty
   */
  public Optional<TemplateInfo> byName(String name, String language) {
    return templates.stream()
        .filter(t -> name != null && name.equals(t.name()))
        .filter(t -> language == null || language.equals(t.language()))
        .findFirst();
  }
}
