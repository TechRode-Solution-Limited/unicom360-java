package com.unicom360;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.unicom360.errors.ApiException;
import com.unicom360.errors.ApiServerException;
import com.unicom360.errors.DuplicateReferenceException;
import com.unicom360.errors.ErrorDetails;
import com.unicom360.errors.NoValidRecipientsException;
import com.unicom360.errors.RateLimitException;
import com.unicom360.errors.SessionWindowClosedException;
import com.unicom360.errors.Unicom360Exception;
import com.unicom360.models.Balance;
import com.unicom360.models.Balances;
import com.unicom360.models.SmsSendResult;
import com.unicom360.models.StatusResult;
import com.unicom360.models.TemplateInfo;
import com.unicom360.models.TemplateList;
import com.unicom360.params.SmsSendParams;
import com.unicom360.params.TemplateRecipient;
import com.unicom360.params.TemplateSendParams;
import com.unicom360.transport.TransportResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModelsAndErrorsTest {
  private static Unicom360Client answering(int status, String body) {
    return Unicom360Client.builder()
        .apiKey("the-key")
        .baseUrl(Unicom360Client.DEFAULT_BASE_URL)
        .maxRetries(0)
        .clock(Clock.fixed(Instant.ofEpochSecond(1767225630), ZoneOffset.UTC))
        .transport(request -> TransportResponse.of(status, body))
        .build();
  }

  private static SmsSendParams sms() {
    return SmsSendParams.builder().from("U").to("260971234567").message("Hi").build();
  }

  private static TemplateSendParams template() {
    return TemplateSendParams.builder()
        .template("t")
        .addRecipient(TemplateRecipient.of("1"))
        .build();
  }

  @Test
  void modelsAreImmutable() {
    SmsSendResult result =
        answering(
                200,
                "{\"api_reference\":\"1\",\"message\":\"ok\",\"success\":true,\"request_id\":{\"a\":1},"
                    + "\"data\":[{\"message_id\":\"m\",\"status\":\"ACKNOWLEDGED\",\"mobile_number\":\"26\"}]}")
            .sms()
            .send(sms());
    assertThrows(UnsupportedOperationException.class, () -> result.results().clear());
    assertThrows(UnsupportedOperationException.class, () -> result.extra().clear());
    result.raw().addProperty("x", 1);
    assertFalse(result.raw().has("x"), "raw() is a copy");
    result.extra().get("request_id").getAsJsonObject().addProperty("x", 1);
    assertFalse(result.extra().get("request_id").getAsJsonObject().has("x"), "extra() is a copy");
    assertEquals("m", result.results().get(0).raw().get("message_id").getAsString());
    assertEquals(
        result,
        new SmsSendResult(
            result.apiReference(),
            result.message(),
            result.results(),
            result.extra(),
            result.raw()),
        "records compare by value");
  }

  @Test
  void balancesAreExactDecimals() {
    Balance balance =
        answering(200, "{\"status\":true,\"balance\":37.10,\"bundle_type\":\"WHATSAPP\"}")
            .balance()
            .get(BundleType.WHATSAPP);
    assertEquals(new BigDecimal("37.10"), balance.balance());
    Balances balances =
        answering(
                200,
                "{\"status\":true,\"balances\":{\"SMS\":1250,\"WHATSAPP\":0.1000000000000000055}}")
            .balance()
            .all();
    assertEquals(new BigDecimal("1250"), balances.balances().get("SMS"));
    assertEquals(new BigDecimal("0.1000000000000000055"), balances.balances().get("WHATSAPP"));
    assertEquals(List.of("SMS", "WHATSAPP"), List.copyOf(balances.balances().keySet()));
  }

  @Test
  void lookupsReturnEmptyWhenNothingMatches() {
    StatusResult status =
        answering(
                200,
                "{\"success\":true,\"data\":{\"results\":[{\"message_id\":\"a\",\"status\":\"READ\"}]}}")
            .whatsapp()
            .status(List.of("a"));
    assertEquals("READ", status.byId("a").orElseThrow().status());
    assertTrue(status.byId("b").isEmpty());
    assertTrue(status.byId(null).isEmpty());

    TemplateList templates =
        answering(
                200,
                "{\"success\":true,\"data\":{\"templates\":["
                    + "{\"name\":\"n\",\"language\":\"en\"},{\"name\":\"n\",\"language\":\"fr\"}]}}")
            .whatsapp()
            .templates();
    assertEquals("en", templates.byName("n").map(TemplateInfo::language).orElseThrow());
    assertEquals("fr", templates.byName("n", "fr").map(TemplateInfo::language).orElseThrow());
    assertTrue(templates.byName("n", "de").isEmpty());
    assertTrue(templates.byName("m").isEmpty());
  }

  @Test
  void byNumberWithoutDigitsIsAnExactMatch() {
    SmsSendResult result =
        answering(
                200,
                "{\"success\":true,\"data\":[{\"message_id\":\"a\",\"status\":\"x\",\"mobile_number\":\"abc\"},"
                    + "{\"message_id\":\"b\",\"status\":\"x\"}]}")
            .sms()
            .send(sms());
    assertEquals("a", result.byNumber("abc").orElseThrow().messageId());
    assertTrue(result.byNumber("ab").isEmpty());
    assertEquals(
        "b", result.byNumber(null).orElseThrow().messageId(), "a null number has no digits");
    assertTrue(result.byNumber("+260").isEmpty());
  }

  @Test
  void aWrongJsonTypeIsNullInTheModelAndKeptInRaw() {
    SmsSendResult result =
        answering(200, "{\"success\":true,\"message\":[\"a\"],\"data\":\"oops\"}")
            .sms()
            .send(sms());
    assertNull(result.message());
    assertTrue(result.results().isEmpty());
    assertEquals("oops", result.raw().get("data").getAsString());
  }

  @Test
  void aDuplicateReferenceNamesTheOriginal() {
    DuplicateReferenceException error =
        assertThrows(
            DuplicateReferenceException.class,
            () ->
                answering(
                        200,
                        "{\"success\":false,\"message\":\"DUPLICATE_MSG_REF\",\"data\":{\"message_id\":\"orig\"}}")
                    .sms()
                    .send(sms()));
    assertEquals("orig", error.originalMessageId());
    assertEquals("api_error", error.type());
    assertInstanceOf(ApiException.class, error);

    assertNull(
        assertThrows(
                DuplicateReferenceException.class,
                () ->
                    answering(
                            200,
                            "{\"success\":false,\"message\":\"DUPLICATE_MSG_REF\",\"data\":{\"message_id\":null}}")
                        .sms()
                        .send(sms()))
            .originalMessageId());
    assertNull(
        assertThrows(
                DuplicateReferenceException.class,
                () ->
                    answering(
                            200, "{\"success\":false,\"message\":\"duplicate user reference id\"}")
                        .sms()
                        .send(sms()))
            .originalMessageId());
  }

  @Test
  void noValidRecipientsCarriesTheirResults() {
    NoValidRecipientsException error =
        assertThrows(
            NoValidRecipientsException.class,
            () ->
                answering(
                        200,
                        "{\"success\":false,\"message\":\"NO_VALID_RECIPIENTS\",\"data\":{\"accepted\":0,\"rejected\":1,"
                            + "\"results\":[{\"status\":\"REJECTED\",\"reason\":\"INVALID_NUMBER\",\"to\":12}]}}")
                    .whatsapp()
                    .sendTemplate(template()));
    assertEquals(1, error.results().size());
    assertEquals("INVALID_NUMBER", error.results().get(0).reason());
    assertEquals("12", error.results().get(0).to());
  }

  @Test
  void aRateLimitSaysHowLongToWait() {
    RateLimitException error =
        assertThrows(
            RateLimitException.class,
            () -> answering(429, "{\"message\":\"RATE_LIMIT_EXCEEDED\"}").balance().all());
    assertEquals(Duration.ofSeconds(30), error.retryAfter());
    assertEquals("rate_limit_error", error.type());
  }

  @Test
  void fieldsOnStatusErrorsAreKept() {
    ApiServerException error =
        assertThrows(
            ApiServerException.class, () -> answering(503, "<html>down</html>").balance().all());
    assertEquals(503, error.httpStatus());
    assertEquals("HTTP 503 Service Unavailable", error.getMessage());
    assertEquals("<html>down</html>", error.rawBody());
    assertEquals("UNKNOWN", error.code());
    assertNull(error.apiReference());
    assertNull(error.data());
  }

  @Test
  void anUnnamedStatusIsStillDescribed() {
    Unicom360Exception error =
        assertThrows(Unicom360Exception.class, () -> answering(599, "").balance().all());
    assertEquals("HTTP 599", error.getMessage());
  }

  @Test
  void aRefusalWithANonStringMessageIsDescribed() {
    ApiException error =
        assertThrows(
            ApiException.class,
            () -> answering(200, "{\"success\":false,\"message\":{\"a\":1}}").sms().send(sms()));
    assertEquals(ErrorCodes.UNKNOWN, error.code());
    assertFalse(error.getMessage().isEmpty());
  }

  @Test
  void exceptionsCanBeBuiltInYourOwnTests() {
    JsonObject data = JsonParser.parseString("{\"message_id\":\"orig\"}").getAsJsonObject();
    ErrorDetails details =
        new ErrorDetails(200, null, "ref", null, data, Map.of("X-Thing", List.of("a")));
    assertEquals("UNKNOWN", details.code());
    assertEquals("", details.rawBody());
    assertEquals(List.of("a"), details.headers().get("x-thing"));
    data.addProperty("message_id", "changed");
    assertEquals("orig", details.data().getAsJsonObject().get("message_id").getAsString());
    assertEquals("orig", new DuplicateReferenceException("m", details, 2).originalMessageId());
    SessionWindowClosedException closed = new SessionWindowClosedException("m", details, 3);
    assertEquals(3, closed.attempts());
    assertEquals("ref", closed.apiReference());
  }

  @Test
  void anInterruptedRetryWaitKeepsTheFlagAndThrowsTheLastError() {
    Unicom360Client client =
        Unicom360Client.builder()
            .apiKey("k")
            .baseUrl(Unicom360Client.DEFAULT_BASE_URL)
            .clock(Clock.fixed(Instant.ofEpochSecond(1767225630), ZoneOffset.UTC))
            .transport(
                request -> TransportResponse.of(429, "{\"message\":\"RATE_LIMIT_EXCEEDED\"}"))
            .sleeper(d -> {
              throw new InterruptedException();
            })
            .build();
    try {
      RateLimitException error =
          assertThrows(RateLimitException.class, () -> client.balance().all());
      assertEquals(1, error.attempts());
      assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      Thread.interrupted();
    }
  }
}
