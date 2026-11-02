package com.unicom360.models;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.internal.Json;
import com.unicom360.internal.Matching;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The result of {@code client.sms().send(...)}: one entry per recipient.
 *
 * <p><b>The results are not in request order past the tenth recipient.</b> Find a recipient with
 * {@link #byNumber(String)}, never by position. A recipient with {@code Invalid mobile number} is
 * part of the result, not an exception.
 *
 * @param apiReference the request's reference: quote it to support
 * @param message the gateway's message ({@code successful})
 * @param results one result per recipient, in the response's order
 * @param extra fields this version does not model
 * @param raw the JSON object this was parsed from
 */
public record SmsSendResult(
    String apiReference,
    String message,
    List<SmsResult> results,
    Map<String, JsonElement> extra,
    JsonObject raw) {

  /**
   * Copies the collections and JSON values, so the record is immutable.
   *
   * @param apiReference the request's reference: quote it to support
   * @param message the gateway's message ({@code successful})
   * @param results one result per recipient, in the response's order
   * @param extra fields this version does not model
   * @param raw the JSON object this was parsed from
   */
  public SmsSendResult {
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
   * The results whose status is exactly {@code ACKNOWLEDGED}, in response order.
   *
   * @return the accepted recipients
   */
  public List<SmsResult> accepted() {
    return results.stream().filter(r -> "ACKNOWLEDGED".equals(r.status())).toList();
  }

  /**
   * Every other result (for example {@code Invalid mobile number}), in response order.
   *
   * @return the recipients that were not accepted
   */
  public List<SmsResult> invalid() {
    return results.stream().filter(r -> !"ACKNOWLEDGED".equals(r.status())).toList();
  }

  /**
   * The first result for {@code number}, compared by digits only ({@code "+260 97 123 4567"} finds
   * {@code 260971234567}). Local formats are not expanded: pass the international form.
   *
   * @param number the number to find
   * @return the result, or empty when no result has that number
   */
  public Optional<SmsResult> byNumber(String number) {
    return results.stream().filter(r -> Matching.sameNumber(r.mobileNumber(), number)).findFirst();
  }
}
