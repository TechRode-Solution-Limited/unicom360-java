package com.unicom360.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.conformance.Harness.FakeTime;
import com.unicom360.conformance.Harness.FakeTransport;
import com.unicom360.errors.ApiException;
import com.unicom360.errors.ApiStatusException;
import com.unicom360.errors.RateLimitException;
import com.unicom360.errors.Unicom360Exception;
import com.unicom360.models.EmailResult;
import com.unicom360.models.EmailSendResult;
import com.unicom360.models.SmsResult;
import com.unicom360.models.SmsSendResult;
import com.unicom360.models.TemplateResult;
import com.unicom360.models.TemplateSendResult;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/** Every vector of {@code responses.json}: a canned response and the result or error it means. */
class ResponsesConformanceTest {
  private static final JsonObject FILE = Harness.load("responses.json");
  private static final Instant DEFAULT_NOW = Instant.ofEpochSecond(1767225630);

  @Test
  void theFileHasOnlyKnownKeys() {
    Harness.checkKeys(FILE, Set.of("version", "description", "vectors"), Set.of());
  }

  @TestFactory
  Stream<DynamicTest> everyVector() {
    return Harness.vectors(FILE).stream()
        .map(v -> DynamicTest.dynamicTest(v.get("id").getAsString(), () -> check(v)));
  }

  private static void check(JsonObject vector) {
    Harness.checkKeys(
        vector,
        Set.of("id", "operation", "http", "expect"),
        Set.of("description", "off_spec", "now_unix"));
    String operation = vector.get("operation").getAsString();
    JsonObject expect = vector.getAsJsonObject("expect");
    Harness.checkKeys(expect, Set.of(), Set.of("result", "helpers", "error"));
    Instant now = vector.has("now_unix") ? Harness.instant(vector.get("now_unix")) : DEFAULT_NOW;
    FakeTransport transport = FakeTransport.answering(vector.getAsJsonObject("http"));
    var client = Harness.client(transport, new FakeTime(now), 0, Duration.ofSeconds(60), () -> 0.5);
    JsonObject params = Harness.sampleParams(operation);

    if (expect.has("result")) {
      Object result = Harness.call(client, operation, params);
      Harness.assertCanonicalEqual(expect.get("result"), Harness.canonical(result));
      if (expect.has("helpers")) {
        checkHelpers(operation, result, expect.getAsJsonObject("helpers"));
      }
      return;
    }
    assertTrue(expect.has("error"), "a vector expects a result or an error");
    Unicom360Exception error =
        assertThrows(Unicom360Exception.class, () -> Harness.call(client, operation, params));
    checkError(expect.getAsJsonObject("error"), error);
  }

  private static void checkError(JsonObject expected, Unicom360Exception error) {
    Harness.checkKeys(
        expected,
        Set.of("type", "class", "code", "message", "api_reference", "http_status", "data_present"),
        Set.of("retry_after"));
    Harness.assertPublic(error);
    assertEquals(expected.get("type").getAsString(), error.type());
    assertEquals(expected.get("class").getAsString(), Harness.canonicalClass(error));
    assertEquals(1, error.attempts());

    String code;
    String apiReference;
    int httpStatus;
    boolean dataPresent;
    if (error instanceof ApiStatusException status) {
      code = status.code();
      apiReference = status.apiReference();
      httpStatus = status.httpStatus();
      dataPresent = status.data() != null;
    } else if (error instanceof ApiException api) {
      code = api.code();
      apiReference = api.apiReference();
      httpStatus = api.httpStatus();
      dataPresent = api.data() != null;
    } else {
      fail("an API error was expected, got " + error);
      return;
    }
    assertEquals(expected.get("code").getAsString(), code);
    assertEquals(
        expected.get("api_reference").isJsonNull()
            ? null
            : expected.get("api_reference").getAsString(),
        apiReference);
    assertEquals(expected.get("http_status").getAsInt(), httpStatus);
    assertEquals(expected.get("data_present").getAsBoolean(), dataPresent);
    if (expected.get("message").isJsonNull()) {
      assertFalse(
          error.getMessage() == null || error.getMessage().isEmpty(), "a non-empty message");
    } else {
      assertEquals(expected.get("message").getAsString(), error.getMessage());
    }
    if (expected.has("retry_after")) {
      Duration retryAfter = ((RateLimitException) error).retryAfter();
      assertEquals(
          0,
          expected
              .get("retry_after")
              .getAsBigDecimal()
              .compareTo(BigDecimal.valueOf(retryAfter.toNanos()).movePointLeft(9)),
          "retry_after " + retryAfter);
    }
  }

  private static void checkHelpers(String operation, Object result, JsonObject helpers) {
    Harness.checkKeys(
        helpers, Set.of(), Set.of("accepted", "invalid", "rejected", "by_number", "by_address"));
    for (Map.Entry<String, JsonElement> helper : helpers.entrySet()) {
      switch (operation + "/" + helper.getKey()) {
        case "sms.send/accepted":
          {
            SmsSendResult r = (SmsSendResult) result;
            assertPositions(helper.getValue(), positions(r.results(), r.accepted()));
            break;
          }
        case "sms.send/invalid":
          {
            SmsSendResult r = (SmsSendResult) result;
            assertPositions(helper.getValue(), positions(r.results(), r.invalid()));
            break;
          }
        case "sms.send/by_number":
          {
            SmsSendResult r = (SmsSendResult) result;
            for (Map.Entry<String, JsonElement> entry :
                helper.getValue().getAsJsonObject().entrySet()) {
              Optional<SmsResult> found = r.byNumber(entry.getKey());
              assertLookup(entry, r.results(), found);
            }
            break;
          }
        case "email.send/accepted":
          {
            EmailSendResult r = (EmailSendResult) result;
            assertPositions(helper.getValue(), positions(r.results(), r.accepted()));
            break;
          }
        case "email.send/invalid":
          {
            EmailSendResult r = (EmailSendResult) result;
            assertPositions(helper.getValue(), positions(r.results(), r.invalid()));
            break;
          }
        case "email.send/by_address":
          {
            EmailSendResult r = (EmailSendResult) result;
            for (Map.Entry<String, JsonElement> entry :
                helper.getValue().getAsJsonObject().entrySet()) {
              Optional<EmailResult> found = r.byAddress(entry.getKey());
              assertLookup(entry, r.results(), found);
            }
            break;
          }
        case "whatsapp.send_template/accepted":
          {
            TemplateSendResult r = (TemplateSendResult) result;
            assertPositions(helper.getValue(), positions(r.results(), r.accepted()));
            break;
          }
        case "whatsapp.send_template/rejected":
          {
            TemplateSendResult r = (TemplateSendResult) result;
            assertPositions(helper.getValue(), positions(r.results(), r.rejected()));
            break;
          }
        case "whatsapp.send_template/by_number":
          {
            TemplateSendResult r = (TemplateSendResult) result;
            for (Map.Entry<String, JsonElement> entry :
                helper.getValue().getAsJsonObject().entrySet()) {
              Optional<TemplateResult> found = r.byNumber(entry.getKey());
              assertLookup(entry, r.results(), found);
            }
            break;
          }
        default:
          fail("a helper this harness does not understand: " + operation + "/" + helper.getKey());
      }
    }
  }

  private static <T> List<Integer> positions(List<T> results, List<T> subset) {
    List<Integer> out = new ArrayList<>();
    for (T item : subset) {
      out.add(indexOfIdentity(results, item));
    }
    return out;
  }

  private static <T> int indexOfIdentity(List<T> list, T item) {
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i) == item) {
        return i;
      }
    }
    return -1;
  }

  private static void assertPositions(JsonElement expected, List<Integer> actual) {
    List<Integer> wanted = new ArrayList<>();
    expected.getAsJsonArray().forEach(e -> wanted.add(e.getAsInt()));
    assertEquals(wanted, actual);
  }

  private static <T> void assertLookup(
      Map.Entry<String, JsonElement> entry, List<T> results, Optional<T> found) {
    Integer position = found.map(item -> indexOfIdentity(results, item)).orElse(null);
    Integer wanted = entry.getValue().isJsonNull() ? null : entry.getValue().getAsInt();
    assertEquals(wanted, position, "lookup of " + entry.getKey());
  }
}
