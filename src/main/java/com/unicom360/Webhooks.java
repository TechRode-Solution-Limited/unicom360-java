package com.unicom360;

import com.google.gson.JsonElement;
import com.unicom360.errors.ConfigurationException;
import com.unicom360.errors.SignatureVerificationException;
import com.unicom360.errors.SignatureVerificationException.Reason;
import com.unicom360.errors.Unicom360Exception;
import com.unicom360.events.EmailDeliveryReport;
import com.unicom360.events.SmsDeliveryReport;
import com.unicom360.events.WebhookEvent;
import com.unicom360.internal.Json;
import com.unicom360.internal.Models;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Verifies and parses webhook deliveries. These helpers need no client.
 *
 * <p>WhatsApp events ({@code message.status}, {@code message.received}, {@code webhook.test}) are
 * signed. Pass the <b>raw body bytes</b>, before any JSON parsing, the request's headers and your
 * webhook secret, the 43-character string from the portal, used as it is: never base64-decode it.
 *
 * <pre>{@code
 * WebhookEvent event;
 * try {
 *   event = Webhooks.constructEvent(rawBody, request::getHeader, secret);
 * } catch (SignatureVerificationException e) {
 *   // answer 401; e.reason() says which check failed
 * }
 * }</pre>
 *
 * <p>A null, empty or whitespace-only secret throws {@link ConfigurationException} before any other
 * check, because anyone can forge a signature with an empty key. That is a fault in your
 * configuration, not a failed verification.
 *
 * <p>Deliveries are retried on any non-2xx (after 30 s, 2 min, 10 min, 30 min, 1 h, 3 h, 6 h and 12
 * h): answer 2xx quickly and drop repeats by {@link WebhookEvent#deliveryId()}.
 *
 * <p>SMS and email delivery reports are <b>unsigned and never retried</b>, so anyone can post one:
 * treat them as hints ({@link #parseSmsCallback(byte[])}, {@link #parseEmailCallback(byte[])}).
 */
public final class Webhooks {
  /** The default tolerance on the timestamp: 300 seconds, in either direction. */
  public static final Duration DEFAULT_TOLERANCE = Duration.ofSeconds(300);

  private static final String SIGNATURE_HEADER = "X-Unicom360-Signature";
  private static final String TIMESTAMP_HEADER = "X-Unicom360-Timestamp";
  private static final String DELIVERY_HEADER = "X-Unicom360-Delivery";
  private static final String SIGNATURE_PREFIX = "sha256=";
  private static final Pattern TIMESTAMP = Pattern.compile("[0-9]+");
  private static final Pattern SIGNATURE = Pattern.compile("sha256=[0-9a-f]{64}");

  private Webhooks() {}

  /**
   * Looks up a request header by name, for a framework whose headers are not a map: pass {@code
   * request::getHeader} (a servlet request) or {@code headers::getFirst} (Spring, {@code
   * com.sun.net.httpserver}). The lookup must ignore case and return the first value.
   */
  @FunctionalInterface
  public interface HeaderLookup {
    /**
     * The first value of a header.
     *
     * @param name the header name
     * @return the value, or {@code null} when it is absent
     */
    String get(String name);
  }

  /**
   * Verifies a delivery with the default tolerance against the system clock.
   *
   * @param rawBody the exact bytes received, before any JSON parsing
   * @param headers the request's headers: values may be strings or lists (the first is used), and
   *     names are matched without regard to case
   * @param secret your webhook secret, used as it is
   * @throws SignatureVerificationException when a check fails; {@code reason()} says which
   * @throws ConfigurationException when the secret is null, empty or only whitespace
   */
  public static void verify(byte[] rawBody, Map<String, ?> headers, String secret) {
    verify(rawBody, lookup(headers), secret);
  }

  /**
   * Verifies a delivery against the system clock.
   *
   * @param rawBody the exact bytes received, before any JSON parsing
   * @param headers the request's headers
   * @param secret your webhook secret, used as it is
   * @param tolerance how far the timestamp may be from your clock, in either direction (inclusive)
   * @throws SignatureVerificationException when a check fails
   * @throws ConfigurationException when the secret is null, empty or only whitespace
   */
  public static void verify(
      byte[] rawBody, Map<String, ?> headers, String secret, Duration tolerance) {
    verify(rawBody, lookup(headers), secret, tolerance);
  }

  /**
   * Verifies a delivery at a given time.
   *
   * @param rawBody the exact bytes received, before any JSON parsing
   * @param headers the request's headers
   * @param secret your webhook secret, used as it is
   * @param tolerance how far the timestamp may be from {@code now}, in either direction (inclusive)
   * @param now the time to verify at
   * @throws SignatureVerificationException when a check fails
   * @throws ConfigurationException when the secret is null, empty or only whitespace
   */
  public static void verify(
      byte[] rawBody, Map<String, ?> headers, String secret, Duration tolerance, Instant now) {
    verify(rawBody, lookup(headers), secret, tolerance, now);
  }

  /**
   * Verifies a delivery with the default tolerance against the system clock.
   *
   * @param rawBody the exact bytes received, before any JSON parsing
   * @param headers looks up the request's headers, for example {@code request::getHeader}
   * @param secret your webhook secret, used as it is
   * @throws SignatureVerificationException when a check fails
   * @throws ConfigurationException when the secret is null, empty or only whitespace
   */
  public static void verify(byte[] rawBody, HeaderLookup headers, String secret) {
    verify(rawBody, headers, secret, DEFAULT_TOLERANCE);
  }

  /**
   * Verifies a delivery against the system clock.
   *
   * @param rawBody the exact bytes received, before any JSON parsing
   * @param headers looks up the request's headers
   * @param secret your webhook secret, used as it is
   * @param tolerance how far the timestamp may be from your clock, in either direction (inclusive)
   * @throws SignatureVerificationException when a check fails
   * @throws ConfigurationException when the secret is null, empty or only whitespace
   */
  public static void verify(
      byte[] rawBody, HeaderLookup headers, String secret, Duration tolerance) {
    verify(rawBody, headers, secret, tolerance, Clock.systemUTC().instant());
  }

  /**
   * Verifies a delivery at a given time. The checks run in this order and the first failure wins:
   * signature present, timestamp present, timestamp is a whole number, signature well formed,
   * timestamp within the tolerance, signature matches (in constant time).
   *
   * @param rawBody the exact bytes received, before any JSON parsing
   * @param headers looks up the request's headers
   * @param secret your webhook secret, used as it is
   * @param tolerance how far the timestamp may be from {@code now}, in either direction (inclusive)
   * @param now the time to verify at
   * @throws SignatureVerificationException when a check fails
   * @throws ConfigurationException when the secret is null, empty or only whitespace
   */
  public static void verify(
      byte[] rawBody, HeaderLookup headers, String secret, Duration tolerance, Instant now) {
    Objects.requireNonNull(rawBody, "rawBody");
    Objects.requireNonNull(headers, "headers");
    Objects.requireNonNull(tolerance, "tolerance");
    Objects.requireNonNull(now, "now");
    if (secret == null || secret.isBlank()) {
      throw new ConfigurationException(
          "The webhook secret is empty or only whitespace: set it to the secret from the portal.");
    }

    String signature = headers.get(SIGNATURE_HEADER);
    if (signature == null || signature.isEmpty()) {
      throw new SignatureVerificationException(Reason.MISSING_SIGNATURE);
    }
    String timestamp = headers.get(TIMESTAMP_HEADER);
    if (timestamp == null || timestamp.isEmpty()) {
      throw new SignatureVerificationException(Reason.MISSING_TIMESTAMP);
    }
    if (!TIMESTAMP.matcher(timestamp).matches()) {
      throw new SignatureVerificationException(Reason.INVALID_TIMESTAMP);
    }
    if (!SIGNATURE.matcher(signature).matches()) {
      throw new SignatureVerificationException(Reason.MALFORMED_SIGNATURE);
    }
    if (!withinTolerance(timestamp, tolerance, now)) {
      throw new SignatureVerificationException(Reason.TIMESTAMP_OUT_OF_TOLERANCE);
    }
    byte[] expected = hmac(rawBody, timestamp, secret);
    byte[] given = hex(signature.substring(SIGNATURE_PREFIX.length()));
    if (!MessageDigest.isEqual(expected, given)) {
      throw new SignatureVerificationException(Reason.SIGNATURE_MISMATCH);
    }
  }

  /**
   * Verifies a delivery with the default tolerance, then parses it.
   *
   * @param rawBody the exact bytes received, before any JSON parsing
   * @param headers the request's headers
   * @param secret your webhook secret, used as it is
   * @return the event: {@code MessageStatusEvent}, {@code MessageReceivedEvent}, {@code TestEvent}
   *     or {@code UnknownEvent}
   * @throws ConfigurationException when the secret is null, empty or only whitespace
   * @throws SignatureVerificationException when a check fails
   * @throws Unicom360Exception when the body is not a JSON object with a string {@code event}
   */
  public static WebhookEvent constructEvent(byte[] rawBody, Map<String, ?> headers, String secret) {
    return constructEvent(rawBody, lookup(headers), secret);
  }

  /**
   * Verifies a delivery, then parses it.
   *
   * @param rawBody the exact bytes received, before any JSON parsing
   * @param headers the request's headers
   * @param secret your webhook secret, used as it is
   * @param tolerance how far the timestamp may be from your clock, in either direction (inclusive)
   * @return the event
   * @throws ConfigurationException when the secret is null, empty or only whitespace
   * @throws SignatureVerificationException when a check fails
   * @throws Unicom360Exception when the body is not a JSON object with a string {@code event}
   */
  public static WebhookEvent constructEvent(
      byte[] rawBody, Map<String, ?> headers, String secret, Duration tolerance) {
    return constructEvent(rawBody, lookup(headers), secret, tolerance);
  }

  /**
   * Verifies a delivery at a given time, then parses it.
   *
   * @param rawBody the exact bytes received, before any JSON parsing
   * @param headers the request's headers
   * @param secret your webhook secret, used as it is
   * @param tolerance how far the timestamp may be from {@code now}, in either direction (inclusive)
   * @param now the time to verify at
   * @return the event
   * @throws ConfigurationException when the secret is null, empty or only whitespace
   * @throws SignatureVerificationException when a check fails
   * @throws Unicom360Exception when the body is not a JSON object with a string {@code event}
   */
  public static WebhookEvent constructEvent(
      byte[] rawBody, Map<String, ?> headers, String secret, Duration tolerance, Instant now) {
    return constructEvent(rawBody, lookup(headers), secret, tolerance, now);
  }

  /**
   * Verifies a delivery with the default tolerance, then parses it.
   *
   * @param rawBody the exact bytes received, before any JSON parsing
   * @param headers looks up the request's headers, for example {@code request::getHeader}
   * @param secret your webhook secret, used as it is
   * @return the event
   * @throws ConfigurationException when the secret is null, empty or only whitespace
   * @throws SignatureVerificationException when a check fails
   * @throws Unicom360Exception when the body is not a JSON object with a string {@code event}
   */
  public static WebhookEvent constructEvent(byte[] rawBody, HeaderLookup headers, String secret) {
    return constructEvent(rawBody, headers, secret, DEFAULT_TOLERANCE);
  }

  /**
   * Verifies a delivery, then parses it.
   *
   * @param rawBody the exact bytes received, before any JSON parsing
   * @param headers looks up the request's headers
   * @param secret your webhook secret, used as it is
   * @param tolerance how far the timestamp may be from your clock, in either direction (inclusive)
   * @return the event
   * @throws ConfigurationException when the secret is null, empty or only whitespace
   * @throws SignatureVerificationException when a check fails
   * @throws Unicom360Exception when the body is not a JSON object with a string {@code event}
   */
  public static WebhookEvent constructEvent(
      byte[] rawBody, HeaderLookup headers, String secret, Duration tolerance) {
    return constructEvent(rawBody, headers, secret, tolerance, Clock.systemUTC().instant());
  }

  /**
   * Verifies a delivery at a given time, then parses it. The event type is chosen by the body's
   * {@code event} (the {@code X-Unicom360-Event} header is informational); {@code deliveryId} is
   * the {@code X-Unicom360-Delivery} header.
   *
   * @param rawBody the exact bytes received, before any JSON parsing
   * @param headers looks up the request's headers
   * @param secret your webhook secret, used as it is
   * @param tolerance how far the timestamp may be from {@code now}, in either direction (inclusive)
   * @param now the time to verify at
   * @return the event
   * @throws ConfigurationException when the secret is null, empty or only whitespace
   * @throws SignatureVerificationException when a check fails
   * @throws Unicom360Exception when the body is not a JSON object with a string {@code event}
   */
  public static WebhookEvent constructEvent(
      byte[] rawBody, HeaderLookup headers, String secret, Duration tolerance, Instant now) {
    verify(rawBody, headers, secret, tolerance, now);
    JsonElement json = Json.parseOrNull(new String(rawBody, StandardCharsets.UTF_8));
    if (json == null
        || !json.isJsonObject()
        || Json.strictString(json.getAsJsonObject(), "event") == null) {
      throw new Unicom360Exception("The webhook body is not a JSON object with a string event.");
    }
    return Models.event(json.getAsJsonObject(), headers.get(DELIVERY_HEADER));
  }

  /**
   * Parses the report posted to an SMS {@code callback_url}. These reports are unsigned and never
   * retried, so anyone can post one: treat them as hints.
   *
   * @param rawBody the request's body
   * @return the report
   * @throws Unicom360Exception when the body is not a JSON object
   */
  public static SmsDeliveryReport parseSmsCallback(byte[] rawBody) {
    Objects.requireNonNull(rawBody, "rawBody");
    return parseSmsCallback(new String(rawBody, StandardCharsets.UTF_8));
  }

  /**
   * Parses the report posted to an SMS {@code callback_url}. These reports are unsigned and never
   * retried, so anyone can post one: treat them as hints.
   *
   * @param rawBody the request's body
   * @return the report
   * @throws Unicom360Exception when the body is not a JSON object
   */
  public static SmsDeliveryReport parseSmsCallback(String rawBody) {
    return Models.smsDeliveryReport(callbackObject(rawBody, "SMS"));
  }

  /**
   * Parses the report posted to an email {@code callback_url}. These reports are unsigned and never
   * retried, so anyone can post one: treat them as hints. The wire sends {@code message_id} as a
   * number; the report has the string the send returned.
   *
   * @param rawBody the request's body
   * @return the report
   * @throws Unicom360Exception when the body is not a JSON object
   */
  public static EmailDeliveryReport parseEmailCallback(byte[] rawBody) {
    Objects.requireNonNull(rawBody, "rawBody");
    return parseEmailCallback(new String(rawBody, StandardCharsets.UTF_8));
  }

  /**
   * Parses the report posted to an email {@code callback_url}. These reports are unsigned and never
   * retried, so anyone can post one: treat them as hints.
   *
   * @param rawBody the request's body
   * @return the report
   * @throws Unicom360Exception when the body is not a JSON object
   */
  public static EmailDeliveryReport parseEmailCallback(String rawBody) {
    return Models.emailDeliveryReport(callbackObject(rawBody, "email"));
  }

  private static com.google.gson.JsonObject callbackObject(String rawBody, String kind) {
    JsonElement json = Json.parseOrNull(rawBody);
    if (json == null || !json.isJsonObject()) {
      throw new Unicom360Exception("The " + kind + " callback body is not a JSON object.");
    }
    return json.getAsJsonObject();
  }

  private static HeaderLookup lookup(Map<String, ?> headers) {
    Objects.requireNonNull(headers, "headers");
    return name -> {
      for (Map.Entry<String, ?> entry : headers.entrySet()) {
        if (name.equalsIgnoreCase(entry.getKey())) {
          return first(entry.getValue());
        }
      }
      return null;
    };
  }

  private static String first(Object value) {
    if (value instanceof Collection<?> values) {
      return values.isEmpty() ? null : first(values.iterator().next());
    }
    if (value instanceof Object[] values) {
      return values.length == 0 ? null : first(values[0]);
    }
    return value == null ? null : value.toString();
  }

  private static boolean withinTolerance(String timestamp, Duration tolerance, Instant now) {
    try {
      Instant signedAt = Instant.ofEpochSecond(Long.parseLong(timestamp));
      return Duration.between(signedAt, now).abs().compareTo(tolerance) <= 0;
    } catch (NumberFormatException | DateTimeException | ArithmeticException e) {
      // Too large for a 64-bit integer (or for Instant): certainly out of tolerance.
      return false;
    }
  }

  private static byte[] hmac(byte[] body, String timestamp, String secret) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      mac.update(timestamp.getBytes(StandardCharsets.US_ASCII));
      mac.update((byte) '.');
      return mac.doFinal(body);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("HmacSHA256 is not available", e);
    }
  }

  private static byte[] hex(String digits) {
    byte[] out = new byte[digits.length() / 2];
    for (int i = 0; i < out.length; i++) {
      out[i] = (byte) Integer.parseInt(digits.substring(2 * i, 2 * i + 2), 16);
    }
    return out;
  }
}
