package com.unicom360.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.Unicom360Client;
import com.unicom360.conformance.Harness.FakeTime;
import com.unicom360.conformance.Harness.FakeTransport;
import com.unicom360.errors.ApiConnectionException;
import com.unicom360.errors.ApiException;
import com.unicom360.errors.ApiStatusException;
import com.unicom360.errors.Unicom360Exception;
import com.unicom360.transport.TransportException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/** Every vector of {@code retries.json}: a sequence of outcomes and the attempts and sleeps. */
class RetriesConformanceTest {
  private static final JsonObject FILE = Harness.load("retries.json");
  private static final double TOLERANCE = 1e-6;

  /** Each vector runs with the jitter at the bottom, the middle and the top of its range. */
  private static final double[] JITTER = {0.0, 0.5, 0.999999};

  @Test
  void theFileHasOnlyKnownKeys() {
    Harness.checkKeys(FILE, Set.of("version", "description", "vectors"), Set.of());
  }

  @TestFactory
  Stream<DynamicTest> everyVector() {
    return Harness.vectors(FILE).stream()
        .map(v ->
            DynamicTest.dynamicTest(
                v.get("id").getAsString(),
                () -> {
                  for (double jitter : JITTER) {
                    check(v, jitter);
                  }
                }));
  }

  private static Object step(JsonObject spec) {
    if (spec.has("http")) {
      Harness.checkKeys(spec, Set.of("http"), Set.of());
      return Harness.httpResponse(spec.getAsJsonObject("http"));
    }
    Harness.checkKeys(spec, Set.of("transport"), Set.of());
    String kind = spec.get("transport").getAsString();
    switch (kind) {
      case "connect_refused":
      case "dns_failure":
        return new TransportException(kind, false, false);
      case "timeout_after_send":
        return new TransportException(kind, true, true);
      case "reset_after_send":
        return new TransportException(kind, true, false);
      default:
        return fail("unknown transport failure " + kind);
    }
  }

  private static void check(JsonObject vector, double jitter) {
    Harness.checkKeys(
        vector,
        Set.of("id", "operation", "params", "now_unix", "config", "steps", "expect"),
        Set.of("description"));
    JsonObject config = vector.getAsJsonObject("config");
    Harness.checkKeys(config, Set.of("max_retries", "max_retry_wait"), Set.of());
    JsonObject expect = vector.getAsJsonObject("expect");
    Harness.checkKeys(expect, Set.of("attempts", "sleeps", "outcome"), Set.of());
    String id = vector.get("id").getAsString() + " (jitter " + jitter + ")";

    List<Object> steps = new ArrayList<>();
    for (JsonElement spec : vector.getAsJsonArray("steps")) {
      steps.add(step(spec.getAsJsonObject()));
    }
    FakeTransport transport = new FakeTransport(steps);
    FakeTime time = new FakeTime(Harness.instant(vector.get("now_unix")));
    Unicom360Client client =
        Harness.client(
            transport,
            time,
            config.get("max_retries").getAsInt(),
            Duration.ofNanos(
                config.get("max_retry_wait").getAsBigDecimal().movePointRight(9).longValueExact()),
            () -> jitter);
    String operation = vector.get("operation").getAsString();
    JsonObject params = vector.getAsJsonObject("params");
    JsonObject outcome = expect.getAsJsonObject("outcome");
    int attempts = expect.get("attempts").getAsInt();

    String kind = outcome.get("kind").getAsString();
    if (kind.equals("result")) {
      Harness.checkKeys(outcome, Set.of("kind"), Set.of());
      assertNotNull(Harness.call(client, operation, params), id);
    } else if (kind.equals("error")) {
      Harness.checkKeys(outcome, Set.of("kind", "type", "class", "code", "http_status"), Set.of());
      Unicom360Exception error =
          assertThrows(Unicom360Exception.class, () -> Harness.call(client, operation, params), id);
      Harness.assertPublic(error);
      assertEquals(outcome.get("type").getAsString(), error.type(), id);
      assertEquals(outcome.get("class").getAsString(), Harness.canonicalClass(error), id);
      assertEquals(attempts, error.attempts(), id);
      checkCodeAndStatus(outcome, error, id);
    } else {
      fail("unknown outcome kind " + kind);
    }

    assertEquals(attempts, transport.requests.size(), id + ": attempts");
    var sleeps = expect.getAsJsonArray("sleeps");
    assertEquals(sleeps.size(), time.sleeps.size(), id + ": sleeps " + time.sleeps);
    for (int i = 0; i < sleeps.size(); i++) {
      JsonObject range = sleeps.get(i).getAsJsonObject();
      double slept = Harness.secondsOf(time.sleeps.get(i));
      assertTrue(
          slept >= range.get("min").getAsDouble() - TOLERANCE
              && slept <= range.get("max").getAsDouble() + TOLERANCE,
          id + ": sleep " + i + " was " + slept + ", expected " + range);
    }
  }

  private static void checkCodeAndStatus(JsonObject outcome, Unicom360Exception error, String id) {
    if (error instanceof ApiStatusException status) {
      assertEquals(outcome.get("code").getAsString(), status.code(), id);
      assertEquals(outcome.get("http_status").getAsInt(), status.httpStatus(), id);
    } else if (error instanceof ApiException api) {
      assertEquals(outcome.get("code").getAsString(), api.code(), id);
      assertEquals(outcome.get("http_status").getAsInt(), api.httpStatus(), id);
    } else {
      assertTrue(error instanceof ApiConnectionException, id);
      assertTrue(outcome.get("code").isJsonNull(), id);
      assertTrue(outcome.get("http_status").isJsonNull(), id);
      assertNotNull(error.getCause(), id + ": a cause");
      assertFalse(error.getMessage().isEmpty(), id);
    }
  }
}
