package com.unicom360.conformance;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.Webhooks;
import com.unicom360.errors.SignatureVerificationException;
import com.unicom360.events.MessageReceivedEvent;
import com.unicom360.events.MessageStatusEvent;
import com.unicom360.events.TestEvent;
import com.unicom360.events.UnknownEvent;
import com.unicom360.events.WebhookEvent;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/** Every vector of {@code webhooks/signatures.json} and {@code webhooks/events.json}. */
class WebhooksConformanceTest {
  private static final JsonObject SIGNATURES = Harness.load("webhooks/signatures.json");
  private static final JsonObject EVENTS = Harness.load("webhooks/events.json");

  @Test
  void theFilesHaveOnlyKnownKeys() {
    Harness.checkKeys(
        SIGNATURES, Set.of("version", "description", "algorithm", "vectors"), Set.of());
    Harness.checkKeys(EVENTS, Set.of("version", "description", "vectors"), Set.of());
  }

  private static Map<String, String> headers(JsonObject vector) {
    Map<String, String> out = new LinkedHashMap<>();
    for (Map.Entry<String, JsonElement> header : vector.getAsJsonObject("headers").entrySet()) {
      out.put(header.getKey(), header.getValue().getAsString());
    }
    return out;
  }

  /** The same headers behind a lookup that ignores case, like a servlet request's. */
  private static Webhooks.HeaderLookup lookup(Map<String, String> headers) {
    return name -> {
      for (Map.Entry<String, String> header : headers.entrySet()) {
        if (header.getKey().equalsIgnoreCase(name)) {
          return header.getValue();
        }
      }
      return null;
    };
  }

  @TestFactory
  Stream<DynamicTest> signatures() {
    return Harness.vectors(SIGNATURES).stream()
        .map(v -> DynamicTest.dynamicTest(v.get("id").getAsString(), () -> checkSignature(v)));
  }

  private static void checkSignature(JsonObject vector) {
    Harness.checkKeys(
        vector,
        Set.of(
            "id",
            "description",
            "secret",
            "headers",
            "body_base64",
            "body_text",
            "now_unix",
            "tolerance",
            "expect"),
        Set.of());
    byte[] body = Base64.getDecoder().decode(vector.get("body_base64").getAsString());
    assertArrayEquals(vector.get("body_text").getAsString().getBytes(StandardCharsets.UTF_8), body);
    Map<String, String> headers = headers(vector);
    String secret = vector.get("secret").getAsString();
    Duration tolerance = Duration.ofSeconds(vector.get("tolerance").getAsLong());
    Instant now = Harness.instant(vector.get("now_unix"));
    JsonObject expect = vector.getAsJsonObject("expect");

    if (expect.get("valid").getAsBoolean()) {
      Harness.checkKeys(expect, Set.of("valid"), Set.of());
      Webhooks.verify(body, headers, secret, tolerance, now);
      Webhooks.verify(body, lookup(headers), secret, tolerance, now);
    } else {
      Harness.checkKeys(expect, Set.of("valid", "reason"), Set.of());
      String reason = expect.get("reason").getAsString();
      SignatureVerificationException error =
          assertThrows(
              SignatureVerificationException.class,
              () -> Webhooks.verify(body, headers, secret, tolerance, now));
      assertEquals(reason, error.reason().value());
      assertEquals(reason, error.reason().toString());
      assertEquals("signature_verification_error", error.type());
      assertEquals(
          reason,
          assertThrows(
                  SignatureVerificationException.class,
                  () -> Webhooks.verify(body, lookup(headers), secret, tolerance, now))
              .reason()
              .value());
    }
  }

  @TestFactory
  Stream<DynamicTest> events() {
    return Harness.vectors(EVENTS).stream()
        .map(v -> DynamicTest.dynamicTest(v.get("id").getAsString(), () -> checkEvent(v)));
  }

  private static void checkEvent(JsonObject vector) {
    Harness.checkKeys(
        vector,
        Set.of("id", "secret", "headers", "body_text", "now_unix", "tolerance", "expect"),
        Set.of("description"));
    JsonObject expect = vector.getAsJsonObject("expect");
    Harness.checkKeys(
        expect, Set.of("kind", "event", "delivery_id", "created_at", "data"), Set.of());
    WebhookEvent event =
        Webhooks.constructEvent(
            vector.get("body_text").getAsString().getBytes(StandardCharsets.UTF_8),
            headers(vector),
            vector.get("secret").getAsString(),
            Duration.ofSeconds(vector.get("tolerance").getAsLong()),
            Harness.instant(vector.get("now_unix")));

    Object data;
    String kind = expect.get("kind").getAsString();
    switch (kind) {
      case "message_status":
        data = assertInstance(MessageStatusEvent.class, event).data();
        break;
      case "message_received":
        data = assertInstance(MessageReceivedEvent.class, event).data();
        break;
      case "test":
        data = assertInstance(TestEvent.class, event).data();
        break;
      case "unknown":
        data = assertInstance(UnknownEvent.class, event).data();
        break;
      default:
        fail("unknown event kind " + kind);
        return;
    }
    assertEquals(expect.get("event").getAsString(), event.event());
    if (expect.get("delivery_id").isJsonNull()) {
      assertNull(event.deliveryId());
    } else {
      assertEquals(expect.get("delivery_id").getAsString(), event.deliveryId());
    }
    if (expect.get("created_at").isJsonNull()) {
      assertNull(event.createdAt());
    } else {
      assertEquals(expect.get("created_at").getAsString(), event.createdAt());
    }
    Harness.assertCanonicalEqual(expect.get("data"), Harness.canonical(data));
  }

  private static <T> T assertInstance(Class<T> type, Object value) {
    assertTrue(
        type.isInstance(value), "expected " + type.getSimpleName() + ", got " + value.getClass());
    return type.cast(value);
  }
}
