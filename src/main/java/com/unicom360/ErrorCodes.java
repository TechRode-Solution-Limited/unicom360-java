package com.unicom360;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The normalised error codes, and the normalisation itself. Exceptions carry a code in {@code
 * code()}, so a sentence and a code from the gateway compare the same way: {@code Invalid From
 * Sender ID} and {@code Invalid Sender} are both {@link #INVALID_SENDER_ID}. A code this version
 * does not name passes through as a string; a message that is not a code is {@link #UNKNOWN}.
 */
public final class ErrorCodes {
  /** The message is not a code this SDK recognises (request-shape sentences, Phoenix details). */
  public static final String UNKNOWN = "UNKNOWN";

  /** The sender ID does not exist. */
  public static final String INVALID_SENDER_ID = "INVALID_SENDER_ID";

  /** The sender ID exists but is not active. */
  public static final String SENDER_ID_NOT_ALLOWED = "SENDER_ID_NOT_ALLOWED";

  /** An empty list, or too many recipients. */
  public static final String INVALID_RECIPIENT_LIMIT = "INVALID_RECIPIENT_LIMIT";

  /** The bundle does not cover the send. */
  public static final String INSUFFICIENT_BUNDLE = "INSUFFICIENT_BUNDLE";

  /** The {@code user_reference_id} was already used for this recipient and sender. */
  public static final String DUPLICATE_REFERENCE = "DUPLICATE_REFERENCE";

  /** A recipient row failed validation (length, non-GSM text, an address, the subject). */
  public static final String INVALID_PARAMETERS = "INVALID_PARAMETERS";

  /** The account has several WhatsApp senders and the request named none. */
  public static final String SENDER_REQUIRED = "SENDER_REQUIRED";

  /** No template has that name. */
  public static final String TEMPLATE_NOT_FOUND = "TEMPLATE_NOT_FOUND";

  /** The template cannot be sent. */
  public static final String TEMPLATE_NOT_APPROVED = "TEMPLATE_NOT_APPROVED";

  /** The template exists in several languages and the request named none. */
  public static final String LANGUAGE_REQUIRED = "LANGUAGE_REQUIRED";

  /** The sender cannot send this template. */
  public static final String TEMPLATE_SENDER_MISMATCH = "TEMPLATE_SENDER_MISMATCH";

  /** Every recipient of a template send was rejected. */
  public static final String NO_VALID_RECIPIENTS = "NO_VALID_RECIPIENTS";

  /** The gateway could not queue the message. Retried on a retry-safe call. */
  public static final String SEND_FAILED = "SEND_FAILED";

  /** The customer's 24-hour service window is closed. */
  public static final String SESSION_WINDOW_CLOSED = "SESSION_WINDOW_CLOSED";

  /** The {@code reply_to} message does not exist. */
  public static final String REPLY_TO_NOT_FOUND = "REPLY_TO_NOT_FOUND";

  /** The message reacted to does not exist. */
  public static final String REACTION_TARGET_NOT_FOUND = "REACTION_TARGET_NOT_FOUND";

  /** The number is not valid. */
  public static final String INVALID_NUMBER = "INVALID_NUMBER";

  /** The {@code user_reference_id} is not valid. */
  public static final String INVALID_USER_REFERENCE_ID = "INVALID_USER_REFERENCE_ID";

  /** The media URL is not an https URL Meta can fetch. */
  public static final String MEDIA_URL_INVALID = "MEDIA_URL_INVALID";

  /** The client of this API key does not exist. */
  public static final String CLIENT_NOT_FOUND = "CLIENT_NOT_FOUND";

  /** The request carried no API key. */
  public static final String MISSING_API_KEY = "MISSING_API_KEY";

  /** The {@code Authorization} header is malformed. */
  public static final String INVALID_AUTHORIZATION = "INVALID_AUTHORIZATION";

  /** The API key is not valid. */
  public static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";

  /** The API key is disabled. */
  public static final String CREDENTIAL_DISABLED = "CREDENTIAL_DISABLED";

  /** The client is disabled. */
  public static final String CLIENT_DISABLED = "CLIENT_DISABLED";

  /** The API key has expired. */
  public static final String CREDENTIAL_EXPIRED = "CREDENTIAL_EXPIRED";

  /** The request's IP address is not on the API key's allowlist. */
  public static final String IP_NOT_ALLOWED = "IP_NOT_ALLOWED";

  /** The API key is over its requests-per-minute limit. */
  public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";

  private static final Map<String, String> ALIASES =
      Map.ofEntries(
          Map.entry("Invalid From Sender ID", INVALID_SENDER_ID),
          Map.entry("Invalid Sender", INVALID_SENDER_ID),
          Map.entry("From Sender ID not allowed", SENDER_ID_NOT_ALLOWED),
          Map.entry("Sender not allowed", SENDER_ID_NOT_ALLOWED),
          Map.entry("Recipient list size is invalid", INVALID_RECIPIENT_LIMIT),
          Map.entry("Invalid Recipient Limit", INVALID_RECIPIENT_LIMIT),
          Map.entry("INSUFFICIENT_BUNDLE: no international SMS bundle", INSUFFICIENT_BUNDLE),
          Map.entry("Insufficient bundle", INSUFFICIENT_BUNDLE),
          Map.entry("INSUFFICIENT_BUNDLE_LIMIT", INSUFFICIENT_BUNDLE),
          Map.entry("duplicate user reference id", DUPLICATE_REFERENCE),
          Map.entry("DUPLICATE_MSG_REF", DUPLICATE_REFERENCE),
          Map.entry("Invalid parameters provided", INVALID_PARAMETERS),
          Map.entry(
              "Missing API key. Provide X-API-Key header or Authorization: Bearer <api_key>",
              MISSING_API_KEY),
          Map.entry(
              "Invalid authorization format. Use 'Bearer <api_key>' or 'Basic <base64>'",
              INVALID_AUTHORIZATION),
          Map.entry("Invalid Basic auth format. Use api_key as username", INVALID_AUTHORIZATION),
          Map.entry("Invalid Base64 encoding in Basic auth", INVALID_AUTHORIZATION));

  private static final Pattern WHOLE_CODE = Pattern.compile("[A-Z][A-Z0-9_]*");
  private static final Pattern CODE_PREFIX = Pattern.compile("^([A-Z][A-Z0-9_]*):\\s");

  private ErrorCodes() {}

  /**
   * The normalised code of a raw gateway message. The first rule that matches applies: an exact
   * alias, a whole-text code, a {@code CODE: text} prefix, otherwise {@link #UNKNOWN}. Matching is
   * exact and case-sensitive; nothing is trimmed.
   *
   * @param message the raw message, or {@code null}
   * @return the code; never {@code null}
   */
  public static String normalize(String message) {
    if (message == null) {
      return UNKNOWN;
    }
    String alias = ALIASES.get(message);
    if (alias != null) {
      return alias;
    }
    if (WHOLE_CODE.matcher(message).matches()) {
      return message;
    }
    Matcher prefix = CODE_PREFIX.matcher(message);
    if (prefix.find()) {
      String code = prefix.group(1);
      return ALIASES.getOrDefault(code, code);
    }
    return UNKNOWN;
  }
}
