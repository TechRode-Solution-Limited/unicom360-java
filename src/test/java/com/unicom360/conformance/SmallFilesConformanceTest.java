package com.unicom360.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.ErrorCodes;
import com.unicom360.Gsm;
import com.unicom360.GsmIssue;
import com.unicom360.NonGsmCharacter;
import com.unicom360.Webhooks;
import com.unicom360.errors.Unicom360Exception;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/** Every vector of {@code error-codes.json}, {@code callbacks.json} and {@code gsm7.json}. */
class SmallFilesConformanceTest {
  private static final JsonObject CODES = Harness.load("error-codes.json");
  private static final JsonObject CALLBACKS = Harness.load("callbacks.json");
  private static final JsonObject GSM = Harness.load("gsm7.json");

  @Test
  void theFilesHaveOnlyKnownKeys() {
    Harness.checkKeys(CODES, Set.of("version", "description", "vectors"), Set.of());
    Harness.checkKeys(CALLBACKS, Set.of("version", "description", "vectors"), Set.of());
    Harness.checkKeys(
        GSM,
        Set.of("version", "description", "charset", "max_length", "unit_length", "vectors"),
        Set.of());
  }

  @TestFactory
  Stream<DynamicTest> errorCodes() {
    return Harness.vectors(CODES).stream()
        .map(v ->
            DynamicTest.dynamicTest(
                v.get("id").getAsString(),
                () -> {
                  Harness.checkKeys(v, Set.of("id", "message", "code", "source"), Set.of("note"));
                  assertEquals(
                      v.get("code").getAsString(),
                      ErrorCodes.normalize(v.get("message").getAsString()));
                }));
  }

  @TestFactory
  Stream<DynamicTest> callbacks() {
    return Harness.vectors(CALLBACKS).stream()
        .map(v -> DynamicTest.dynamicTest(v.get("id").getAsString(), () -> checkCallback(v)));
  }

  private static void checkCallback(JsonObject vector) {
    Harness.checkKeys(
        vector, Set.of("id", "kind", "body_text", "expect"), Set.of("description", "off_spec"));
    String kind = vector.get("kind").getAsString();
    String body = vector.get("body_text").getAsString();
    JsonObject expect = vector.getAsJsonObject("expect");
    Harness.checkKeys(expect, Set.of(), Set.of("result", "error"));
    if (expect.has("error")) {
      assertTrue(expect.get("error").getAsBoolean());
      assertThrows(Unicom360Exception.class, () -> parse(kind, body));
    } else {
      Harness.assertCanonicalEqual(expect.get("result"), Harness.canonical(parse(kind, body)));
    }
  }

  private static Object parse(String kind, String body) {
    switch (kind) {
      case "sms":
        return Webhooks.parseSmsCallback(body);
      case "email":
        return Webhooks.parseEmailCallback(body);
      default:
        return fail("unknown callback kind " + kind);
    }
  }

  @Test
  void gsmConstantsMatchTheFile() {
    assertEquals(GSM.get("charset").getAsString(), Gsm.CHARSET);
    assertEquals(GSM.get("max_length").getAsInt(), Gsm.MAX_LENGTH);
    assertEquals(GSM.get("unit_length").getAsInt(), Gsm.UNIT_LENGTH);
  }

  @TestFactory
  Stream<DynamicTest> gsm7() {
    return Harness.vectors(GSM).stream()
        .map(v -> DynamicTest.dynamicTest(v.get("id").getAsString(), () -> checkGsm(v)));
  }

  private static void checkGsm(JsonObject vector) {
    Harness.checkKeys(
        vector,
        Set.of("id", "text", "is_gsm7", "length", "units", "non_gsm", "validate"),
        Set.of("description"));
    String text = vector.get("text").getAsString();
    assertEquals(vector.get("is_gsm7").getAsBoolean(), Gsm.isGsm7(text), "isGsm7");
    assertEquals(vector.get("length").getAsInt(), Gsm.length(text), "length");
    assertEquals(vector.get("units").getAsInt(), Gsm.units(text), "units");

    List<NonGsmCharacter> characters = Gsm.nonGsmCharacters(text);
    JsonArray expected = vector.getAsJsonArray("non_gsm");
    assertEquals(expected.size(), characters.size(), "nonGsmCharacters");
    for (int i = 0; i < expected.size(); i++) {
      JsonObject item = expected.get(i).getAsJsonObject();
      Harness.checkKeys(item, Set.of("char", "index"), Set.of());
      assertEquals(item.get("char").getAsString(), characters.get(i).character());
      assertEquals(item.get("index").getAsInt(), characters.get(i).index());
    }

    List<String> issues = new ArrayList<>();
    for (GsmIssue issue : Gsm.validateSms(text)) {
      issues.add(issue.value());
      assertEquals(issue.value(), issue.toString());
    }
    List<String> wanted = new ArrayList<>();
    for (JsonElement issue : vector.getAsJsonArray("validate")) {
      wanted.add(issue.getAsString());
    }
    assertEquals(wanted, issues, "validateSms");
  }
}
