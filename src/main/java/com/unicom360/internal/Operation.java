package com.unicom360.internal;

/** The nine operations of the public API. Not part of the public API. */
public enum Operation {
  /** {@code sms.send}. */
  SMS_SEND("POST", "/api/v1/json/bulk/sms", Retry.WITH_REFERENCE),
  /** {@code email.send}. */
  EMAIL_SEND("POST", "/api/v1/json/bulk/email", Retry.NEVER),
  /** {@code whatsapp.send_template}. */
  WHATSAPP_SEND_TEMPLATE("POST", "/api/v1/json/whatsapp/template", Retry.EVERY_RECIPIENT_REFERENCE),
  /** {@code whatsapp.send_session}. */
  WHATSAPP_SEND_SESSION("POST", "/api/v1/json/whatsapp/session", Retry.WITH_REFERENCE),
  /** {@code whatsapp.status}. */
  WHATSAPP_STATUS("POST", "/api/v1/json/whatsapp/status", Retry.ALWAYS),
  /** {@code whatsapp.templates}. */
  WHATSAPP_TEMPLATES("GET", "/api/v1/json/whatsapp/templates", Retry.ALWAYS),
  /** {@code balance.get}. */
  BALANCE_GET("POST", "/api/v1/json/balance", Retry.ALWAYS),
  /** {@code balance.all}. */
  BALANCE_ALL("POST", "/api/v1/json/balance/all", Retry.ALWAYS),
  /** {@code health}. */
  HEALTH("GET", "/health", Retry.ALWAYS);

  /** When an operation is retry-safe (the spec's {@code x-retry-safe}). */
  public enum Retry {
    /** Read operations. */
    ALWAYS,
    /** Safe when the call has a {@code user_reference_id} with a non-whitespace character. */
    WITH_REFERENCE,
    /** Safe when every recipient has such a {@code user_reference_id}. */
    EVERY_RECIPIENT_REFERENCE,
    /** Email: its reference check has no unique index behind it. */
    NEVER
  }

  private final String method;
  private final String path;
  private final Retry retry;

  Operation(String method, String path, Retry retry) {
    this.method = method;
    this.path = path;
    this.retry = retry;
  }

  /** The HTTP method. */
  public String method() {
    return method;
  }

  /** The path, appended to the base URL. */
  public String path() {
    return path;
  }

  /** The retry-safety rule. */
  public Retry retry() {
    return retry;
  }

  /** Whether the call carries the API key (all but {@code health}). */
  public boolean authenticated() {
    return this != HEALTH;
  }

  /** Whether the envelope uses {@code status} rather than {@code success}. */
  public boolean balance() {
    return this == BALANCE_GET || this == BALANCE_ALL;
  }

  /** Whether this operation sends a message. */
  public boolean send() {
    return retry != Retry.ALWAYS;
  }
}
