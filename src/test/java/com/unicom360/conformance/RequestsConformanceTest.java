package com.unicom360.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.unicom360.conformance.Harness.FakeTime;
import com.unicom360.conformance.Harness.FakeTransport;
import com.unicom360.transport.TransportRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/** Every vector of {@code requests.json}: an SDK call and the exact HTTP request it must make. */
class RequestsConformanceTest {
  private static final JsonObject FILE = Harness.load("requests.json");

  @Test
  void theFileHasOnlyKnownKeys() {
    Harness.checkKeys(
        FILE, Set.of("version", "description", "canned_responses", "vectors"), Set.of());
    assertEquals(
        Set.copyOf(Harness.OPERATIONS),
        FILE.getAsJsonObject("canned_responses").keySet(),
        "one canned response per operation");
  }

  @TestFactory
  Stream<DynamicTest> everyVector() {
    return Harness.vectors(FILE).stream()
        .map(v -> DynamicTest.dynamicTest(v.get("id").getAsString(), () -> check(v)));
  }

  private static void check(JsonObject vector) {
    Harness.checkKeys(vector, Set.of("id", "operation", "params", "expect"), Set.of("description"));
    JsonObject expect = vector.getAsJsonObject("expect");
    Harness.checkKeys(
        expect,
        Set.of(
            "method",
            "path",
            "headers",
            "absent_headers",
            "user_agent_prefix",
            "json",
            "absent_keys"),
        Set.of());
    String operation = vector.get("operation").getAsString();

    FakeTransport transport =
        FakeTransport.answering(
            FILE.getAsJsonObject("canned_responses").getAsJsonObject(operation));
    FakeTime time = new FakeTime(Instant.ofEpochSecond(1767225630));
    Object result =
        Harness.call(
            Harness.client(transport, time, 0, Duration.ofSeconds(60), () -> 0.5),
            operation,
            vector.getAsJsonObject("params"));
    assertNotNull(result);

    assertEquals(1, transport.requests.size(), "exactly one request");
    TransportRequest request = transport.requests.get(0);
    assertEquals(expect.get("method").getAsString(), request.method());
    assertEquals(expect.get("path").getAsString(), request.uri().getPath());
    assertNull(request.uri().getQuery(), "no query string");

    for (Map.Entry<String, JsonElement> header : expect.getAsJsonObject("headers").entrySet()) {
      String wanted = header.getValue().getAsString().replace("<KEY>", Harness.API_KEY);
      assertEquals(wanted, request.header(header.getKey()), header.getKey());
    }
    for (JsonElement absent : expect.getAsJsonArray("absent_headers")) {
      assertNull(request.header(absent.getAsString()), absent.getAsString() + " must be absent");
    }
    String userAgent = request.header("User-Agent");
    assertNotNull(userAgent);
    assertTrue(
        userAgent.startsWith(expect.get("user_agent_prefix").getAsString()),
        "User-Agent: " + userAgent);

    JsonElement expected = expect.get("json");
    if (expected instanceof JsonNull) {
      assertTrue(request.body() == null || request.body().length == 0, "no body");
    } else {
      assertNotNull(request.body(), "a body");
      JsonElement actual = JsonParser.parseString(request.bodyText());
      String problem = Harness.difference(expected, actual, "$");
      assertNull(problem, problem);
      for (JsonElement pointer : expect.getAsJsonArray("absent_keys")) {
        assertFalse(
            Harness.pointerExists(actual, pointer.getAsString()),
            pointer.getAsString() + " must be absent");
      }
    }
  }
}
