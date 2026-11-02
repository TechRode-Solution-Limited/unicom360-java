package com.unicom360;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.unicom360.errors.Unicom360Exception;
import com.unicom360.internal.Models;
import com.unicom360.internal.Operation;
import com.unicom360.models.SessionSendResult;
import com.unicom360.models.StatusResult;
import com.unicom360.models.TemplateList;
import com.unicom360.models.TemplateSendResult;
import com.unicom360.params.SessionMessage;
import com.unicom360.params.TemplateSendParams;
import java.util.List;
import java.util.Objects;

/** The WhatsApp operations: {@code client.whatsapp()}. */
public final class WhatsAppService {
  private final Unicom360Client client;

  WhatsAppService(Unicom360Client client) {
    this.client = client;
  }

  /**
   * Sends one approved template to 1 to 1,000 recipients, each with its own parameters. A rejected
   * recipient is part of the result; when every recipient is rejected this throws {@link
   * com.unicom360.errors.NoValidRecipientsException}. Retry-safe when every recipient has a {@code
   * user_reference_id}.
   *
   * @param params the template and its recipients
   * @return one result per recipient, in request order
   * @throws Unicom360Exception when the call fails or the gateway refuses it
   */
  public TemplateSendResult sendTemplate(TemplateSendParams params) {
    return sendTemplate(params, null);
  }

  /**
   * Sends one approved template to 1 to 1,000 recipients.
   *
   * @param params the template and its recipients
   * @param options overrides for this call, or {@code null}
   * @return one result per recipient, in request order
   * @throws Unicom360Exception when the call fails or the gateway refuses it
   */
  public TemplateSendResult sendTemplate(TemplateSendParams params, RequestOptions options) {
    Objects.requireNonNull(params, "params");
    return client.execute(
        Operation.WHATSAPP_SEND_TEMPLATE, params.toJson(), options, Models::templateSendResult);
  }

  /**
   * Sends one message inside the 24-hour customer service window. Outside the window this throws
   * {@link com.unicom360.errors.SessionWindowClosedException}: send a template instead. Retry-safe
   * with a {@code user_reference_id}.
   *
   * @param message the message, built with the {@link SessionMessage} factories
   * @return the result
   * @throws Unicom360Exception when the call fails or the gateway refuses it
   */
  public SessionSendResult sendSession(SessionMessage message) {
    return sendSession(message, null);
  }

  /**
   * Sends one message inside the 24-hour customer service window.
   *
   * @param message the message, built with the {@link SessionMessage} factories
   * @param options overrides for this call, or {@code null}
   * @return the result
   * @throws Unicom360Exception when the call fails or the gateway refuses it
   */
  public SessionSendResult sendSession(SessionMessage message, RequestOptions options) {
    Objects.requireNonNull(message, "message");
    return client.execute(
        Operation.WHATSAPP_SEND_SESSION, message.toJson(), options, Models::sessionSendResult);
  }

  /**
   * The status of 1 to 100 messages. The ids are sent as given; the gateway answers a repeated id
   * once and an unknown id as {@code NOT_FOUND}.
   *
   * @param messageIds the {@code message_id}s the sends returned
   * @return the statuses
   * @throws Unicom360Exception when the call fails or the gateway refuses it
   */
  public StatusResult status(List<String> messageIds) {
    return status(messageIds, null);
  }

  /**
   * The status of 1 to 100 messages.
   *
   * @param messageIds the {@code message_id}s the sends returned
   * @param options overrides for this call, or {@code null}
   * @return the statuses
   * @throws Unicom360Exception when the call fails or the gateway refuses it
   */
  public StatusResult status(List<String> messageIds, RequestOptions options) {
    Objects.requireNonNull(messageIds, "messageIds");
    JsonArray ids = new JsonArray();
    messageIds.forEach(ids::add);
    JsonObject body = new JsonObject();
    body.add("message_ids", ids);
    return client.execute(Operation.WHATSAPP_STATUS, body, options, Models::statusResult);
  }

  /**
   * The approved templates.
   *
   * @return the templates
   * @throws Unicom360Exception when the call fails or the gateway refuses it
   */
  public TemplateList templates() {
    return templates(null);
  }

  /**
   * The approved templates.
   *
   * @param options overrides for this call, or {@code null}
   * @return the templates
   * @throws Unicom360Exception when the call fails or the gateway refuses it
   */
  public TemplateList templates(RequestOptions options) {
    return client.execute(Operation.WHATSAPP_TEMPLATES, null, options, Models::templateList);
  }
}
