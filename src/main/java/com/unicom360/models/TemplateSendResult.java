package com.unicom360.models;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.internal.Json;
import com.unicom360.internal.Matching;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The result of {@code client.whatsapp().sendTemplate(...)}: one entry per recipient, in request
 * order.
 *
 * <p>A rejected recipient is not an exception. When every recipient is rejected the call throws
 * {@link com.unicom360.errors.NoValidRecipientsException} instead.
 *
 * @param apiReference the request's reference: quote it to support
 * @param message the gateway's message ({@code successful})
 * @param acceptedCount how many recipients were queued (the API's {@code accepted})
 * @param rejectedCount how many recipients were rejected (the API's {@code rejected})
 * @param results one result per recipient, in request order
 * @param extra fields this version does not model
 * @param raw the JSON object this was parsed from
 */
public record TemplateSendResult(
    String apiReference,
    String message,
    int acceptedCount,
    int rejectedCount,
    List<TemplateResult> results,
    Map<String, JsonElement> extra,
    JsonObject raw) {

  /**
   * Copies the collections and JSON values, so the record is immutable.
   *
   * @param apiReference the request's reference: quote it to support
   * @param message the gateway's message ({@code successful})
   * @param acceptedCount how many recipients were queued (the API's {@code accepted})
   * @param rejectedCount how many recipients were rejected (the API's {@code rejected})
   * @param results one result per recipient, in request order
   * @param extra fields this version does not model
   * @param raw the JSON object this was parsed from
   */
  public TemplateSendResult {
    results = results == null ? List.of() : List.copyOf(results);
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
   * The results whose status is exactly {@code ACKNOWLEDGED}.
   *
   * @return the queued recipients
   */
  public List<TemplateResult> accepted() {
    return results.stream().filter(r -> "ACKNOWLEDGED".equals(r.status())).toList();
  }

  /**
   * The results whose status is exactly {@code REJECTED}; {@link TemplateResult#reason()} says why.
   *
   * @return the rejected recipients
   */
  public List<TemplateResult> rejected() {
    return results.stream().filter(r -> "REJECTED".equals(r.status())).toList();
  }

  /**
   * The first result for {@code number}, compared by digits only.
   *
   * @param number the number to find
   * @return the result, or empty when no result has that number
   */
  public Optional<TemplateResult> byNumber(String number) {
    return results.stream().filter(r -> Matching.sameNumber(r.to(), number)).findFirst();
  }
}
