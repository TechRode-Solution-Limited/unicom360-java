package com.unicom360.models;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.internal.Json;
import java.util.List;
import java.util.Map;

/**
 * One approved WhatsApp template.
 *
 * @param name the template's name
 * @param language its language code, for example {@code en_US}
 * @param category {@code MARKETING}, {@code UTILITY} or {@code AUTHENTICATION}; other values pass
 *     through
 * @param from the number it was made on (any sender of the same account can send it), or {@code
 *     null}
 * @param header the header, or {@code null}
 * @param body the body, or {@code null}
 * @param footer the footer, or {@code null}
 * @param buttons the buttons
 * @param authentication the code settings of an {@code AUTHENTICATION} template, or {@code null}
 * @param extra fields this version does not model
 * @param raw the JSON object this was parsed from
 */
public record TemplateInfo(
    String name,
    String language,
    String category,
    String from,
    TemplateHeaderInfo header,
    TemplateBodyInfo body,
    String footer,
    List<TemplateButtonInfo> buttons,
    TemplateAuthentication authentication,
    Map<String, JsonElement> extra,
    JsonObject raw) {

  /**
   * Copies the collections and JSON values, so the record is immutable.
   *
   * @param name the template's name
   * @param language its language code, for example {@code en_US}
   * @param category {@code MARKETING}, {@code UTILITY} or {@code AUTHENTICATION}; other values pass
   *     through
   * @param from the number it was made on (any sender of the same account can send it), or {@code
   *     null}
   * @param header the header, or {@code null}
   * @param body the body, or {@code null}
   * @param footer the footer, or {@code null}
   * @param buttons the buttons
   * @param authentication the code settings of an {@code AUTHENTICATION} template, or {@code null}
   * @param extra fields this version does not model
   * @param raw the JSON object this was parsed from
   */
  public TemplateInfo {
    buttons = buttons == null ? List.of() : List.copyOf(buttons);
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
