package com.unicom360.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.ErrorCodes;
import com.unicom360.errors.ApiException;
import com.unicom360.errors.ApiServerException;
import com.unicom360.errors.ApiStatusException;
import com.unicom360.errors.AuthenticationException;
import com.unicom360.errors.DuplicateReferenceException;
import com.unicom360.errors.ErrorDetails;
import com.unicom360.errors.InsufficientBundleException;
import com.unicom360.errors.InvalidRequestException;
import com.unicom360.errors.NoValidRecipientsException;
import com.unicom360.errors.RateLimitException;
import com.unicom360.errors.SessionWindowClosedException;
import com.unicom360.errors.Unicom360Exception;
import com.unicom360.transport.TransportResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns one HTTP response into a result body or a failure (docs/sdk-design.md section 7). Not part
 * of the public API.
 */
public final class Interpreter {
  private Interpreter() {}

  /** What one HTTP response came to: a body to parse, or a failure. */
  public static final class Outcome {
    private final JsonObject body;
    private final Failure failure;

    private Outcome(JsonObject body, Failure failure) {
      this.body = body;
      this.failure = failure;
    }

    /** The result body, or {@code null} on a failure. */
    public JsonObject body() {
      return body;
    }

    /** The failure, or {@code null} on a result. */
    public Failure failure() {
      return failure;
    }
  }

  /** A response that is an error; becomes an exception once the attempts are known. */
  public static final class Failure {
    private final boolean api;
    private final String message;
    private final ErrorDetails details;
    private final Duration retryAfter;

    private Failure(boolean api, String message, ErrorDetails details, Duration retryAfter) {
      this.api = api;
      this.message = message;
      this.details = details;
      this.retryAfter = retryAfter;
    }

    /** The {@code api_reference} the body carried, or {@code null}. */
    public String apiReference() {
      return details.apiReference();
    }

    /** Whether this is a 429. */
    public boolean rateLimited() {
      return details.httpStatus() == 429;
    }

    /** Whether this is a 5xx. */
    public boolean serverError() {
      return details.httpStatus() >= 500 && details.httpStatus() <= 599;
    }

    /** Whether this is a refusal with the code {@code SEND_FAILED}. */
    public boolean sendFailed() {
      return api && ErrorCodes.SEND_FAILED.equals(details.code());
    }

    /** A short description for the retry log line: never a body or a parameter. */
    public String describe() {
      return (api ? "api_error" : "api_status_error")
          + "(HTTP "
          + details.httpStatus()
          + ", "
          + details.code()
          + ")";
    }

    /** The exception for this failure. */
    public Unicom360Exception toException(int attempts) {
      if (api) {
        switch (details.code()) {
          case ErrorCodes.INSUFFICIENT_BUNDLE:
            return new InsufficientBundleException(message, details, attempts);
          case ErrorCodes.DUPLICATE_REFERENCE:
            return new DuplicateReferenceException(message, details, attempts);
          case ErrorCodes.NO_VALID_RECIPIENTS:
            return new NoValidRecipientsException(message, details, attempts);
          case ErrorCodes.SESSION_WINDOW_CLOSED:
            return new SessionWindowClosedException(message, details, attempts);
          default:
            return new ApiException(message, details, attempts);
        }
      }
      int status = details.httpStatus();
      if (status == 400) {
        return new InvalidRequestException(message, details, attempts);
      }
      if (status == 401) {
        return new AuthenticationException(message, details, attempts);
      }
      if (status == 429) {
        return new RateLimitException(message, details, retryAfter, attempts);
      }
      if (status >= 500 && status <= 599) {
        return new ApiServerException(message, details, attempts);
      }
      return new ApiStatusException(message, details, attempts);
    }
  }

  /** The time from {@code now} to the next UTC minute, in (0, 60] seconds. */
  public static Duration untilNextMinute(Instant now) {
    return Duration.ofSeconds(60 - Math.floorMod(now.getEpochSecond(), 60L))
        .minusNanos(now.getNano());
  }

  /** Interprets one response. */
  public static Outcome interpret(Operation operation, TransportResponse response, Instant now) {
    int status = response.status();
    String text = response.body();
    JsonElement json = Json.parseOrNull(text);
    JsonObject object = json != null && json.isJsonObject() ? json.getAsJsonObject() : null;

    if (status >= 200 && status <= 299) {
      if (object == null) {
        return statusFailure(
            phrase(status) + ": the response is not a Unicom360 JSON object", response);
      }
      if (operation == Operation.HEALTH) {
        return new Outcome(object, null);
      }
      String flagName = operation.balance() ? "status" : "success";
      Boolean flag = Json.bool(object, flagName);
      if (Boolean.TRUE.equals(flag)) {
        return new Outcome(object, null);
      }
      if (flag == null) {
        return statusFailure(
            phrase(status) + ": the response has no boolean " + flagName + " flag", response);
      }
      String message = messageText(object.get("message"));
      String code = ErrorCodes.normalize(message);
      ErrorDetails details =
          new ErrorDetails(
              status,
              code,
              Json.strictString(object, "api_reference"),
              text,
              object.get("data"),
              response.headers());
      return new Outcome(
          null,
          new Failure(
              true,
              message != null ? message : "The request was refused (" + phrase(status) + ")",
              details,
              null));
    }

    String message = null;
    String code = ErrorCodes.UNKNOWN;
    String apiReference = null;
    JsonElement data = null;
    if (object != null && object.has("message")) {
      message = messageText(object.get("message"));
      code = ErrorCodes.normalize(message);
      apiReference = Json.strictString(object, "api_reference");
      data = object.get("data");
    } else if (object != null) {
      JsonObject errors = Json.object(object, "errors");
      message = errors == null ? null : Json.strictString(errors, "detail");
    }
    ErrorDetails details =
        new ErrorDetails(status, code, apiReference, text, data, response.headers());
    return new Outcome(
        null,
        new Failure(
            false,
            message != null ? message : phrase(status),
            details,
            status == 429 ? untilNextMinute(now) : null));
  }

  /** A 2xx without a usable envelope. */
  private static Outcome statusFailure(String message, TransportResponse response) {
    ErrorDetails details =
        new ErrorDetails(
            response.status(), ErrorCodes.UNKNOWN, null, response.body(), null, response.headers());
    return new Outcome(null, new Failure(false, message, details, null));
  }

  /** A string as is; an array of strings joined with {@code "; "}; anything else {@code null}. */
  public static String messageText(JsonElement value) {
    if (value == null) {
      return null;
    }
    if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
      return value.getAsString();
    }
    if (value.isJsonArray()) {
      JsonArray array = value.getAsJsonArray();
      List<String> parts = new ArrayList<>(array.size());
      for (JsonElement element : array) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
          return null;
        }
        parts.add(element.getAsString());
      }
      return String.join("; ", parts);
    }
    return null;
  }

  private static String phrase(int status) {
    String reason = reason(status);
    return reason == null ? "HTTP " + status : "HTTP " + status + " " + reason;
  }

  private static String reason(int status) {
    switch (status) {
      case 200:
        return "OK";
      case 201:
        return "Created";
      case 202:
        return "Accepted";
      case 204:
        return "No Content";
      case 301:
        return "Moved Permanently";
      case 302:
        return "Found";
      case 303:
        return "See Other";
      case 307:
        return "Temporary Redirect";
      case 308:
        return "Permanent Redirect";
      case 400:
        return "Bad Request";
      case 401:
        return "Unauthorized";
      case 403:
        return "Forbidden";
      case 404:
        return "Not Found";
      case 405:
        return "Method Not Allowed";
      case 406:
        return "Not Acceptable";
      case 408:
        return "Request Timeout";
      case 413:
        return "Content Too Large";
      case 415:
        return "Unsupported Media Type";
      case 429:
        return "Too Many Requests";
      case 500:
        return "Internal Server Error";
      case 502:
        return "Bad Gateway";
      case 503:
        return "Service Unavailable";
      case 504:
        return "Gateway Timeout";
      default:
        return null;
    }
  }
}
