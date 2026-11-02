package com.unicom360.models;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.internal.Json;
import com.unicom360.internal.Matching;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The result of {@code client.email().send(...)}: one entry per address.
 *
 * <p><b>The results are not in request order past the tenth address.</b> Find an address with
 * {@link #byAddress(String)}.
 *
 * @param apiReference the request's reference: quote it to support
 * @param message the gateway's message ({@code successful})
 * @param results one result per address, in the response's order
 * @param extra fields this version does not model
 * @param raw the JSON object this was parsed from
 */
public record EmailSendResult(
    String apiReference,
    String message,
    List<EmailResult> results,
    Map<String, JsonElement> extra,
    JsonObject raw) {

  /**
   * Copies the collections and JSON values, so the record is immutable.
   *
   * @param apiReference the request's reference: quote it to support
   * @param message the gateway's message ({@code successful})
   * @param results one result per address, in the response's order
   * @param extra fields this version does not model
   * @param raw the JSON object this was parsed from
   */
  public EmailSendResult {
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
   * @return the accepted addresses
   */
  public List<EmailResult> accepted() {
    return results.stream().filter(r -> "ACKNOWLEDGED".equals(r.status())).toList();
  }

  /**
   * Every other result (for example {@code Invalid email address}), in response order.
   *
   * @return the addresses that were not accepted
   */
  public List<EmailResult> invalid() {
    return results.stream().filter(r -> !"ACKNOWLEDGED".equals(r.status())).toList();
  }

  /**
   * The first result for {@code address}, ignoring case and surrounding white space.
   *
   * @param address the address to find
   * @return the result, or empty when no result has that address
   */
  public Optional<EmailResult> byAddress(String address) {
    return results.stream()
        .filter(r -> Matching.sameAddress(r.emailAddress(), address))
        .findFirst();
  }
}
