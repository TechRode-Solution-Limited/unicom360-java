package com.unicom360;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.unicom360.errors.ConfigurationException;
import com.unicom360.errors.SignatureVerificationException;
import com.unicom360.errors.SignatureVerificationException.Reason;
import com.unicom360.errors.Unicom360Exception;
import com.unicom360.events.MessageStatusEvent;
import com.unicom360.events.TestEvent;
import com.unicom360.events.UnknownEvent;
import com.unicom360.events.WebhookEvent;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class WebhooksExtraTest {
  private static final String SECRET = "krISEvfh1iUWHg5sdWQd0ZnOvE6D6mrCsuFLNEuhGfI";
  private static final Instant NOW = Instant.ofEpochSecond(1792829762);
  private static final String TIMESTAMP = Long.toString(NOW.getEpochSecond());

  private static String sign(String timestamp, byte[] body, String secret)
      throws GeneralSecurityException {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
    return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
  }

  private static byte[] bytes(String text) {
    return text.getBytes(StandardCharsets.UTF_8);
  }

  private static Map<String, String> signed(byte[] body) throws GeneralSecurityException {
    return Map.of(
        "X-Unicom360-Timestamp",
        TIMESTAMP,
        "X-Unicom360-Signature",
        sign(TIMESTAMP, body, SECRET),
        "X-Unicom360-Delivery",
        "42");
  }

  private static WebhookEvent construct(byte[] body) throws GeneralSecurityException {
    return Webhooks.constructEvent(body, signed(body), SECRET, Webhooks.DEFAULT_TOLERANCE, NOW);
  }

  @Test
  void headerValuesMayBeListsOrArraysAndTheFirstIsUsed() throws GeneralSecurityException {
    byte[] body = bytes("{}");
    String signature = sign(TIMESTAMP, body, SECRET);
    Map<String, List<String>> lists =
        Map.of(
            "x-unicom360-timestamp", List.of(TIMESTAMP, "1"),
            "X-UNICOM360-SIGNATURE", List.of(signature, "sha256=0"));
    assertDoesNotThrow(() -> Webhooks.verify(body, lists, SECRET, Webhooks.DEFAULT_TOLERANCE, NOW));
    Map<String, String[]> arrays =
        Map.of(
            "X-Unicom360-Timestamp", new String[] {TIMESTAMP},
            "X-Unicom360-Signature", new String[] {signature});
    assertDoesNotThrow(
        () -> Webhooks.verify(body, arrays, SECRET, Webhooks.DEFAULT_TOLERANCE, NOW));
    Map<String, List<String>> empty =
        Map.of("X-Unicom360-Timestamp", List.of(TIMESTAMP), "X-Unicom360-Signature", List.of());
    assertEquals(
        Reason.MISSING_SIGNATURE,
        assertThrows(
                SignatureVerificationException.class,
                () -> Webhooks.verify(body, empty, SECRET, Webhooks.DEFAULT_TOLERANCE, NOW))
            .reason());
  }

  /** Check 3 before check 4: no conformance vector has both problems at once. */
  @Test
  void anInvalidTimestampIsReportedBeforeAMalformedSignature() {
    Map<String, String> headers =
        Map.of("X-Unicom360-Timestamp", "yesterday", "X-Unicom360-Signature", "not-a-signature");
    assertEquals(
        Reason.INVALID_TIMESTAMP,
        assertThrows(
                SignatureVerificationException.class,
                () ->
                    Webhooks.verify(bytes("{}"), headers, SECRET, Webhooks.DEFAULT_TOLERANCE, NOW))
            .reason());
  }

  @Test
  void aTimestampTooLargeFor64BitsIsOutOfTolerance() throws GeneralSecurityException {
    byte[] body = bytes("{}");
    for (String timestamp :
        List.of("9223372036854775807", "9223372036854775808", "99999999999999999999999999")) {
      Map<String, String> headers =
          Map.of(
              "X-Unicom360-Timestamp",
              timestamp,
              "X-Unicom360-Signature",
              sign(timestamp, body, SECRET));
      assertEquals(
          Reason.TIMESTAMP_OUT_OF_TOLERANCE,
          assertThrows(
                  SignatureVerificationException.class,
                  () -> Webhooks.verify(body, headers, SECRET, Webhooks.DEFAULT_TOLERANCE, NOW))
              .reason(),
          timestamp);
    }
  }

  @Test
  void aSubSecondClockIsComparedExactly() throws GeneralSecurityException {
    byte[] body = bytes("{}");
    Map<String, String> headers = signed(body);
    Duration tolerance = Duration.ofSeconds(300);
    assertDoesNotThrow(
        () -> Webhooks.verify(body, headers, SECRET, tolerance, NOW.plusSeconds(300)));
    assertThrows(
        SignatureVerificationException.class,
        () ->
            Webhooks.verify(body, headers, SECRET, tolerance, NOW.plusSeconds(300).plusMillis(1)));
  }

  @Test
  void theSecretIsNeverBase64DecodedOrPrinted() {
    byte[] body = bytes("{}");
    Map<String, String> headers =
        Map.of(
            "X-Unicom360-Timestamp",
            TIMESTAMP,
            "X-Unicom360-Signature",
            "sha256=" + "0".repeat(64));
    SignatureVerificationException error =
        assertThrows(
            SignatureVerificationException.class,
            () -> Webhooks.verify(body, headers, SECRET, Webhooks.DEFAULT_TOLERANCE, NOW));
    assertEquals(Reason.SIGNATURE_MISMATCH, error.reason());
    assertFalse(error.getMessage().contains(SECRET));
    assertFalse(error.toString().contains(SECRET));
  }

  @Test
  void anEmptySecretIsAConfigurationProblem() {
    assertThrows(
        ConfigurationException.class,
        () -> Webhooks.verify(bytes("{}"), Map.of(), "", Webhooks.DEFAULT_TOLERANCE, NOW));
  }

  private static final List<String> BLANK_SECRETS =
      List.of("", " ", "   ", "\t", "\r\n", " \t\r\n ");

  /**
   * The signature of {@code {}} at {@link #TIMESTAMP} under the empty key, which {@code
   * SecretKeySpec} will not take: {@code printf '1792829762.{}' | openssl dgst -sha256 -hmac ''}.
   */
  private static final String EMPTY_KEY_SIGNATURE =
      "sha256=8a6fe206ee11571e4a054be02ac4011eb0a25d370e01944f0d3b2fd1d4369314";

  /** Must never verify: anyone can compute the signature. */
  @Test
  void aBlankSecretRefusesARequestSignedWithThatKey() throws GeneralSecurityException {
    byte[] body = bytes("{}");
    for (String blank : BLANK_SECRETS) {
      String signature = blank.isEmpty() ? EMPTY_KEY_SIGNATURE : sign(TIMESTAMP, body, blank);
      Map<String, String> forged =
          Map.of("X-Unicom360-Timestamp", TIMESTAMP, "X-Unicom360-Signature", signature);
      assertThrows(
          ConfigurationException.class,
          () -> Webhooks.verify(body, forged, blank, Webhooks.DEFAULT_TOLERANCE, NOW));
      assertThrows(
          ConfigurationException.class,
          () -> Webhooks.constructEvent(body, forged, blank, Webhooks.DEFAULT_TOLERANCE, NOW));
    }
  }

  @Test
  void anUnsetSecretIsAConfigurationProblem() {
    String unset = null; // System.getenv("UNICOM360_WEBHOOK_SECRET")
    assertThrows(ConfigurationException.class, () -> Webhooks.verify(bytes("{}"), Map.of(), unset));
    assertThrows(
        ConfigurationException.class, () -> Webhooks.constructEvent(bytes("{}"), Map.of(), unset));
  }

  @Test
  void aBlankSecretIsRefusedBeforeAnyHeaderCheck() {
    for (String blank : BLANK_SECRETS) {
      assertThrows(
          ConfigurationException.class, () -> Webhooks.verify(bytes("{}"), Map.of(), blank));
      assertThrows(
          ConfigurationException.class,
          () -> Webhooks.constructEvent(bytes("{}"), Map.of(), blank));
    }
  }

  @Test
  void theSecretIsUsedExactlyAsGiven() throws GeneralSecurityException {
    byte[] body = bytes("{}");
    String padded = " " + SECRET + "\n";
    Map<String, String> headers =
        Map.of(
            "X-Unicom360-Timestamp",
            TIMESTAMP,
            "X-Unicom360-Signature",
            sign(TIMESTAMP, body, padded));
    assertDoesNotThrow(
        () -> Webhooks.verify(body, headers, padded, Webhooks.DEFAULT_TOLERANCE, NOW));
    assertEquals(
        Reason.SIGNATURE_MISMATCH,
        assertThrows(
                SignatureVerificationException.class,
                () -> Webhooks.verify(body, headers, SECRET, Webhooks.DEFAULT_TOLERANCE, NOW))
            .reason());
  }

  @Test
  void aSignedBodyThatIsNotAnEventIsTheBaseException() {
    for (String text : List.of("not json", "[]", "{}", "{\"event\":1}", "")) {
      Unicom360Exception error =
          assertThrows(Unicom360Exception.class, () -> construct(bytes(text)), text);
      assertEquals(Unicom360Exception.class, error.getClass(), text);
      assertEquals("unicom360_error", error.type());
    }
  }

  @Test
  void aBadSignatureIsReportedBeforeTheBodyIsParsed() {
    Map<String, String> headers =
        Map.of(
            "X-Unicom360-Timestamp",
            TIMESTAMP,
            "X-Unicom360-Signature",
            "sha256=" + "0".repeat(64));
    assertThrows(
        SignatureVerificationException.class,
        () ->
            Webhooks.constructEvent(
                bytes("not json"), headers, SECRET, Webhooks.DEFAULT_TOLERANCE, NOW));
  }

  @Test
  void eventsCarryTheDeliveryIdAndRaw() throws GeneralSecurityException {
    WebhookEvent event =
        construct(bytes("{\"event\":\"webhook.test\",\"data\":{\"message\":\"hi\"},\"x\":1}"));
    assertTrue(event instanceof TestEvent);
    assertEquals("42", event.deliveryId());
    assertNull(event.createdAt());
    assertEquals(1, event.raw().get("x").getAsInt());
    assertEquals("hi", ((TestEvent) event).data().message());
    assertEquals(1, ((TestEvent) event).extra().get("x").getAsInt());
  }

  @Test
  void theEventTypeComesFromTheBodyNotTheHeader() throws GeneralSecurityException {
    byte[] body = bytes("{\"event\":\"message.status\",\"data\":{\"status\":\"sent\"}}");
    Map<String, String> headers =
        Map.of(
            "X-Unicom360-Timestamp",
            TIMESTAMP,
            "X-Unicom360-Signature",
            sign(TIMESTAMP, body, SECRET),
            "X-Unicom360-Event",
            "webhook.test");
    WebhookEvent event =
        Webhooks.constructEvent(body, headers, SECRET, Webhooks.DEFAULT_TOLERANCE, NOW);
    assertEquals("sent", ((MessageStatusEvent) event).data().status());
    assertNull(event.deliveryId(), "no delivery header");
  }

  @Test
  void aKnownEventWithoutDataHasNullData() throws GeneralSecurityException {
    assertNull(((MessageStatusEvent) construct(bytes("{\"event\":\"message.status\"}"))).data());
  }

  @Test
  void anUnknownEventKeepsAnyDataAndNeverRaises() throws GeneralSecurityException {
    assertEquals(
        3,
        ((UnknownEvent) construct(bytes("{\"event\":\"x\",\"data\":[1,2,3]}")))
            .data()
            .getAsJsonArray()
            .size());
    assertNull(((UnknownEvent) construct(bytes("{\"event\":\"x\",\"data\":null}"))).data());
    assertNull(((UnknownEvent) construct(bytes("{\"event\":\"x\"}"))).data());
  }

  @Test
  void payloadCopiesCannotChangeTheEvent() throws GeneralSecurityException {
    WebhookEvent event = construct(bytes("{\"event\":\"x\",\"data\":{\"a\":1}}"));
    JsonObject raw = event.raw();
    raw.addProperty("a", 2);
    assertFalse(event.raw().has("a"));
  }

  @Test
  void callbacksTakeBytesToo() {
    assertEquals(
        "48213",
        Webhooks.parseEmailCallback(bytes("{\"message_id\":48213,\"status\":\"SENT\"}"))
            .messageId());
    assertEquals(
        "SENT",
        Webhooks.parseSmsCallback(bytes("{\"message_id\":\"a\",\"status\":\"SENT\"}")).status());
  }
}
