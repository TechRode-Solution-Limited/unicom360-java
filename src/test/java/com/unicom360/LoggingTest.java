package com.unicom360;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.unicom360.errors.Unicom360Exception;
import com.unicom360.params.SmsSendParams;
import com.unicom360.transport.Transport;
import com.unicom360.transport.TransportException;
import com.unicom360.transport.TransportResponse;
import java.lang.System.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;

class LoggingTest {
  private static final String KEY = "s3cret-api-key";
  private static final String NUMBER = "260971234567";
  private static final String TEXT = "Your private text";

  /** Records every line with its level, at the levels it is told to enable. */
  private static final class Recording implements Logger {
    private final Level least;
    final List<String> lines = new ArrayList<>();

    Recording(Level least) {
      this.least = least;
    }

    @Override
    public String getName() {
      return "recording";
    }

    @Override
    public boolean isLoggable(Level level) {
      return level.getSeverity() >= least.getSeverity();
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
      lines.add(level + " " + msg);
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String format, Object... params) {
      lines.add(level + " " + format);
    }
  }

  private static Unicom360Client client(Transport transport, Logger logger) {
    return Unicom360Client.builder()
        .apiKey(KEY)
        .baseUrl(Unicom360Client.DEFAULT_BASE_URL)
        .autoReferenceIds(true)
        .transport(transport)
        .logger(logger)
        .sleeper(d -> {})
        .random(() -> 0.5)
        .build();
  }

  private static SmsSendParams sms() {
    return SmsSendParams.builder().from("Unicom360").to(NUMBER).message(TEXT).build();
  }

  private static void assertNoSecrets(List<String> lines) {
    for (String line : lines) {
      assertFalse(line.contains(KEY), line);
      assertFalse(line.contains(NUMBER), line);
      assertFalse(line.contains(TEXT), line);
    }
  }

  @Test
  void oneDebugLinePerAttemptAndOneInfoLinePerRetry() {
    Recording logger = new Recording(Logger.Level.DEBUG);
    int[] calls = {0};
    Transport transport =
        request -> {
          calls[0]++;
          if (calls[0] == 1) {
            throw new TransportException("reset", true, false);
          }
          return TransportResponse.of(
              200, "{\"api_reference\":\"ref-1\",\"message\":\"m\",\"success\":true,\"data\":[]}");
        };
    client(transport, logger).sms().send(sms());
    assertEquals(3, logger.lines.size(), logger.lines.toString());

    String first = logger.lines.get(0);
    assertTrue(
        first.startsWith("DEBUG unicom360 POST /api/v1/json/bulk/sms status=connection_error"),
        first);
    assertTrue(first.contains("api_reference=- attempt=1 duration_ms="), first);
    assertTrue(first.contains("generated_user_reference_ids=u360-"), first);

    String retry = logger.lines.get(1);
    assertTrue(
        retry.startsWith("INFO unicom360 retrying POST /api/v1/json/bulk/sms after attempt=1"),
        retry);
    assertTrue(retry.contains("wait_s=0.500"), retry);
    assertTrue(retry.contains("cause=connection_error"), retry);

    String second = logger.lines.get(2);
    assertTrue(second.startsWith("DEBUG unicom360 POST /api/v1/json/bulk/sms status=200"), second);
    assertTrue(second.contains("api_reference=ref-1 attempt=2"), second);
    assertNoSecrets(logger.lines);
  }

  @Test
  void aRetriedHttpFailureNamesItsCause() {
    Recording logger = new Recording(Logger.Level.INFO);
    int[] calls = {0};
    Transport transport =
        request -> {
          calls[0]++;
          return calls[0] == 1
              ? TransportResponse.of(503, "<html>" + NUMBER + "</html>")
              : TransportResponse.of(200, "{\"success\":true,\"data\":[]}");
        };
    client(transport, logger).sms().send(sms());
    assertEquals(1, logger.lines.size(), logger.lines.toString());
    assertTrue(
        logger.lines.get(0).contains("cause=api_status_error(HTTP 503, UNKNOWN)"),
        logger.lines.get(0));
    assertNoSecrets(logger.lines);
  }

  @Test
  void nothingIsFormattedWhenTheLevelIsOff() {
    Recording logger = new Recording(Logger.Level.ERROR);
    Transport transport = request -> TransportResponse.of(400, "{\"message\":\"x\"}");
    assertThrows(Unicom360Exception.class, () -> client(transport, logger).sms().send(sms()));
    assertTrue(logger.lines.isEmpty());
  }

  @Test
  void aFailureNeverCarriesTheKey() {
    Recording logger = new Recording(Logger.Level.TRACE);
    Transport transport =
        request -> {
          throw new IllegalStateException("boom");
        };
    Unicom360Exception error =
        assertThrows(Unicom360Exception.class, () -> client(transport, logger).sms().send(sms()));
    assertFalse(error.getMessage().contains(KEY));
    assertNoSecrets(logger.lines);
  }
}
