package com.unicom360.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.events.EmailDeliveryReport;
import com.unicom360.events.InboundMedia;
import com.unicom360.events.InboundMessage;
import com.unicom360.events.MessageReceivedEvent;
import com.unicom360.events.MessageStatusData;
import com.unicom360.events.MessageStatusEvent;
import com.unicom360.events.SmsDeliveryReport;
import com.unicom360.events.TestEvent;
import com.unicom360.events.TestEventData;
import com.unicom360.events.UnknownEvent;
import com.unicom360.events.WebhookEvent;
import com.unicom360.models.Balance;
import com.unicom360.models.Balances;
import com.unicom360.models.EmailResult;
import com.unicom360.models.EmailSendResult;
import com.unicom360.models.Health;
import com.unicom360.models.MessageError;
import com.unicom360.models.MessageStatus;
import com.unicom360.models.SessionSendResult;
import com.unicom360.models.SmsResult;
import com.unicom360.models.SmsSendResult;
import com.unicom360.models.StatusResult;
import com.unicom360.models.TemplateAuthentication;
import com.unicom360.models.TemplateBodyInfo;
import com.unicom360.models.TemplateButtonInfo;
import com.unicom360.models.TemplateHeaderInfo;
import com.unicom360.models.TemplateInfo;
import com.unicom360.models.TemplateList;
import com.unicom360.models.TemplateResult;
import com.unicom360.models.TemplateSendResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Builds the typed models from parsed JSON. Lenient by design: a field of the wrong JSON type is
 * {@code null} in the model (and still in {@code raw}). Not part of the public API.
 */
public final class Models {
  private static final Set<String> ENVELOPE = Set.of("success", "data", "api_reference", "message");

  private Models() {}

  private static JsonObject data(JsonObject envelope) {
    return Json.object(envelope, "data");
  }

  private static <T> List<T> list(JsonElement value, Function<JsonObject, T> item) {
    List<T> out = new ArrayList<>();
    if (value != null && value.isJsonArray()) {
      for (JsonElement element : value.getAsJsonArray()) {
        if (element.isJsonObject()) {
          out.add(item.apply(element.getAsJsonObject()));
        }
      }
    }
    return out;
  }

  private static List<String> strings(JsonElement value) {
    List<String> out = new ArrayList<>();
    if (value != null && value.isJsonArray()) {
      JsonArray array = value.getAsJsonArray();
      for (JsonElement element : array) {
        String text = Json.string(element);
        if (text != null) {
          out.add(text);
        }
      }
    }
    return out;
  }

  private static Map<String, JsonElement> merge(
      Map<String, JsonElement> outer, Map<String, JsonElement> inner) {
    Map<String, JsonElement> out = new LinkedHashMap<>(outer);
    inner.forEach(out::putIfAbsent);
    return out;
  }

  // ------------------------------------------------------------------------------- operations

  /** {@code sms.send}. */
  public static SmsSendResult smsSendResult(JsonObject envelope) {
    return new SmsSendResult(
        Json.strictString(envelope, "api_reference"),
        Json.string(envelope, "message"),
        list(envelope.get("data"), Models::smsResult),
        Json.extras(envelope, ENVELOPE),
        envelope);
  }

  private static SmsResult smsResult(JsonObject o) {
    return new SmsResult(
        Json.string(o, "message_id"),
        Json.string(o, "status"),
        Json.string(o, "mobile_number"),
        Json.extras(o, Set.of("message_id", "status", "mobile_number")),
        o);
  }

  /** {@code email.send}. */
  public static EmailSendResult emailSendResult(JsonObject envelope) {
    return new EmailSendResult(
        Json.strictString(envelope, "api_reference"),
        Json.string(envelope, "message"),
        list(envelope.get("data"), Models::emailResult),
        Json.extras(envelope, ENVELOPE),
        envelope);
  }

  private static EmailResult emailResult(JsonObject o) {
    return new EmailResult(
        Json.string(o, "message_id"),
        Json.string(o, "status"),
        Json.string(o, "email_address"),
        Json.extras(o, Set.of("message_id", "status", "email_address")),
        o);
  }

  /** {@code whatsapp.send_template}. */
  public static TemplateSendResult templateSendResult(JsonObject envelope) {
    JsonObject data = data(envelope);
    Integer accepted = Json.integer(data, "accepted");
    Integer rejected = Json.integer(data, "rejected");
    return new TemplateSendResult(
        Json.strictString(envelope, "api_reference"),
        Json.string(envelope, "message"),
        accepted == null ? 0 : accepted,
        rejected == null ? 0 : rejected,
        templateResults(data == null ? null : data.get("results")),
        merge(
            Json.extras(envelope, ENVELOPE),
            Json.extras(data, Set.of("accepted", "rejected", "results"))),
        envelope);
  }

  /**
   * The template results in {@code value}: a list of result objects, or an object whose {@code
   * results} is that list (a {@code NO_VALID_RECIPIENTS} refusal's {@code data}).
   */
  public static List<TemplateResult> templateResults(JsonElement value) {
    JsonElement list = value;
    if (value != null && value.isJsonObject()) {
      list = value.getAsJsonObject().get("results");
    }
    return list(list, Models::templateResult);
  }

  private static TemplateResult templateResult(JsonObject o) {
    return new TemplateResult(
        Json.string(o, "message_id"),
        Json.string(o, "to"),
        Json.string(o, "user_reference_id"),
        Json.string(o, "status"),
        Json.string(o, "reason"),
        Json.string(o, "category"),
        Json.extras(
            o, Set.of("message_id", "to", "user_reference_id", "status", "reason", "category")),
        o);
  }

  /** {@code whatsapp.send_session}. */
  public static SessionSendResult sessionSendResult(JsonObject envelope) {
    JsonObject data = data(envelope);
    return new SessionSendResult(
        Json.strictString(envelope, "api_reference"),
        Json.string(envelope, "message"),
        Json.string(data, "message_id"),
        Json.string(data, "to"),
        Json.string(data, "user_reference_id"),
        Json.string(data, "status"),
        Json.string(data, "category"),
        merge(
            Json.extras(envelope, ENVELOPE),
            Json.extras(
                data, Set.of("message_id", "to", "user_reference_id", "status", "category"))),
        envelope);
  }

  /** {@code whatsapp.status}. */
  public static StatusResult statusResult(JsonObject envelope) {
    JsonObject data = data(envelope);
    return new StatusResult(
        Json.strictString(envelope, "api_reference"),
        Json.string(envelope, "message"),
        list(data == null ? null : data.get("results"), Models::messageStatus),
        merge(Json.extras(envelope, ENVELOPE), Json.extras(data, Set.of("results"))),
        envelope);
  }

  private static MessageStatus messageStatus(JsonObject o) {
    return new MessageStatus(
        Json.string(o, "message_id"),
        Json.string(o, "status"),
        Json.string(o, "user_reference_id"),
        Json.string(o, "to"),
        Json.string(o, "from"),
        Json.string(o, "sent_at"),
        Json.string(o, "delivered_at"),
        Json.string(o, "read_at"),
        messageError(Json.object(o, "error")),
        Json.string(o, "category"),
        Json.bool(o, "billable"),
        Json.bool(o, "refunded"),
        Json.extras(
            o,
            Set.of(
                "message_id",
                "status",
                "user_reference_id",
                "to",
                "from",
                "sent_at",
                "delivered_at",
                "read_at",
                "error",
                "category",
                "billable",
                "refunded")),
        o);
  }

  private static MessageError messageError(JsonObject o) {
    if (o == null) {
      return null;
    }
    return new MessageError(
        Json.integer(o, "code"),
        Json.string(o, "message"),
        Json.extras(o, Set.of("code", "message")),
        o);
  }

  /** {@code whatsapp.templates}. */
  public static TemplateList templateList(JsonObject envelope) {
    JsonObject data = data(envelope);
    return new TemplateList(
        Json.strictString(envelope, "api_reference"),
        Json.string(envelope, "message"),
        list(data == null ? null : data.get("templates"), Models::templateInfo),
        merge(Json.extras(envelope, ENVELOPE), Json.extras(data, Set.of("templates"))),
        envelope);
  }

  private static TemplateInfo templateInfo(JsonObject o) {
    JsonObject header = Json.object(o, "header");
    JsonObject body = Json.object(o, "body");
    JsonObject authentication = Json.object(o, "authentication");
    return new TemplateInfo(
        Json.string(o, "name"),
        Json.string(o, "language"),
        Json.string(o, "category"),
        Json.string(o, "from"),
        header == null
            ? null
            : new TemplateHeaderInfo(
                Json.string(header, "type"),
                strings(header.get("variables")),
                Json.extras(header, Set.of("type", "variables")),
                header),
        body == null
            ? null
            : new TemplateBodyInfo(
                Json.string(body, "text"),
                strings(body.get("variables")),
                Json.extras(body, Set.of("text", "variables")),
                body),
        Json.string(o, "footer"),
        list(o.get("buttons"), Models::templateButtonInfo),
        authentication == null
            ? null
            : new TemplateAuthentication(
                Json.string(authentication, "otp_type"),
                Json.integer(authentication, "code_expiration_minutes"),
                Json.extras(authentication, Set.of("otp_type", "code_expiration_minutes")),
                authentication),
        Json.extras(
            o,
            Set.of(
                "name",
                "language",
                "category",
                "from",
                "header",
                "body",
                "footer",
                "buttons",
                "authentication")),
        o);
  }

  private static TemplateButtonInfo templateButtonInfo(JsonObject o) {
    Integer index = Json.integer(o, "index");
    return new TemplateButtonInfo(
        index == null ? 0 : index,
        Json.string(o, "type"),
        Json.string(o, "text"),
        Json.string(o, "url"),
        Json.string(o, "otp_type"),
        Json.extras(o, Set.of("index", "type", "text", "url", "otp_type")),
        o);
  }

  /** {@code balance.get}. */
  public static Balance balance(JsonObject body) {
    return new Balance(
        Json.string(body, "message"),
        Json.decimal(body.get("balance")),
        Json.string(body, "bundle_type"),
        Json.string(body, "client_id"),
        Json.string(body, "client_name"),
        Json.extras(
            body,
            Set.of("status", "message", "balance", "bundle_type", "client_id", "client_name")),
        body);
  }

  /** {@code balance.all}. */
  public static Balances balances(JsonObject body) {
    Map<String, BigDecimal> balances = new LinkedHashMap<>();
    JsonObject values = Json.object(body, "balances");
    if (values != null) {
      for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
        BigDecimal value = Json.decimal(entry.getValue());
        if (value != null) {
          balances.put(entry.getKey(), value);
        }
      }
    }
    return new Balances(
        Json.string(body, "message"),
        balances,
        Json.string(body, "client_id"),
        Json.string(body, "client_name"),
        Json.extras(body, Set.of("status", "message", "balances", "client_id", "client_name")),
        body);
  }

  /** {@code health}. */
  public static Health health(JsonObject body) {
    return new Health(
        Json.string(body, "status"),
        Json.string(body, "timestamp"),
        Json.string(body, "version"),
        Json.string(body, "environment"),
        Json.extras(body, Set.of("status", "timestamp", "version", "environment")),
        body);
  }

  // ----------------------------------------------------------------------------------- events

  private static final Set<String> EVENT_KEYS = Set.of("event", "created_at", "data");

  /** A verified webhook body: the event model for its {@code event}. */
  public static WebhookEvent event(JsonObject body, String deliveryId) {
    String event = Json.strictString(body, "event");
    String createdAt = Json.strictString(body, "created_at");
    JsonObject data = Json.object(body, "data");
    Map<String, JsonElement> extra = Json.extras(body, EVENT_KEYS);
    switch (event) {
      case "message.status":
        return new MessageStatusEvent(
            event, deliveryId, createdAt, messageStatusData(data), extra, body);
      case "message.received":
        return new MessageReceivedEvent(
            event, deliveryId, createdAt, inboundMessage(data), extra, body);
      case "webhook.test":
        return new TestEvent(
            event,
            deliveryId,
            createdAt,
            data == null
                ? null
                : new TestEventData(
                    Json.string(data, "message"), Json.extras(data, Set.of("message")), data),
            extra,
            body);
      default:
        JsonElement raw = body.get("data");
        return new UnknownEvent(
            event,
            deliveryId,
            createdAt,
            raw == null || raw.isJsonNull() ? null : raw,
            extra,
            body);
    }
  }

  private static MessageStatusData messageStatusData(JsonObject o) {
    if (o == null) {
      return null;
    }
    return new MessageStatusData(
        Json.string(o, "message_id"),
        Json.string(o, "user_reference_id"),
        Json.string(o, "channel"),
        Json.string(o, "to"),
        Json.string(o, "from"),
        Json.string(o, "status"),
        Json.string(o, "timestamp"),
        messageError(Json.object(o, "error")),
        Json.string(o, "category"),
        Json.bool(o, "billable"),
        Json.extras(
            o,
            Set.of(
                "message_id",
                "user_reference_id",
                "channel",
                "to",
                "from",
                "status",
                "timestamp",
                "error",
                "category",
                "billable")),
        o);
  }

  private static InboundMessage inboundMessage(JsonObject o) {
    if (o == null) {
      return null;
    }
    JsonObject media = Json.object(o, "media");
    return new InboundMessage(
        Json.string(o, "message_id"),
        Json.string(o, "channel"),
        Json.string(o, "from"),
        Json.string(o, "to"),
        Json.string(o, "type"),
        Json.string(o, "text"),
        media == null
            ? null
            : new InboundMedia(
                Json.string(media, "id"),
                Json.string(media, "mime_type"),
                Json.string(media, "filename"),
                Json.extras(media, Set.of("id", "mime_type", "filename")),
                media),
        Json.object(o, "payload"),
        Json.string(o, "reply_to"),
        Json.string(o, "timestamp"),
        Json.string(o, "window_expires_at"),
        Json.extras(
            o,
            Set.of(
                "message_id",
                "channel",
                "from",
                "to",
                "type",
                "text",
                "media",
                "payload",
                "reply_to",
                "timestamp",
                "window_expires_at")),
        o);
  }

  /** An SMS delivery report. */
  public static SmsDeliveryReport smsDeliveryReport(JsonObject o) {
    return new SmsDeliveryReport(
        Json.string(o, "message_id"),
        Json.string(o, "status"),
        Json.string(o, "mobile_number"),
        Json.extras(o, Set.of("message_id", "status", "mobile_number")),
        o);
  }

  /** An email delivery report. */
  public static EmailDeliveryReport emailDeliveryReport(JsonObject o) {
    return new EmailDeliveryReport(
        Json.string(o, "message_id"),
        Json.string(o, "status"),
        Json.string(o, "email_address"),
        Json.string(o, "message"),
        Json.extras(o, Set.of("message_id", "status", "email_address", "message")),
        o);
  }
}
