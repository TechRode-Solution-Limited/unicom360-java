package com.unicom360.params;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;

/**
 * The parameters of {@code client.email().send(...)}.
 *
 * <pre>{@code
 * EmailSendParams params = EmailSendParams.builder()
 *     .from("noreply@yourdomain.com")
 *     .to("jane@example.com")
 *     .subject("Your order is confirmed")
 *     .emailBody("<p>Thanks for your order.</p>")
 *     .build();
 * }</pre>
 */
public final class EmailSendParams {
  private final String from;
  private final Recipients to;
  private final String subject;
  private final String emailBody;
  private final String replyTo;
  private final String userReferenceId;
  private final String callbackUrl;
  private final String source;

  private EmailSendParams(Builder b) {
    this.from = b.from;
    this.to = b.to;
    this.subject = b.subject;
    this.emailBody = b.emailBody;
    this.replyTo = b.replyTo;
    this.userReferenceId = b.userReferenceId;
    this.callbackUrl = b.callbackUrl;
    this.source = b.source;
  }

  /**
   * A new builder.
   *
   * @return the builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder holding these parameters.
   *
   * @return the builder
   */
  public Builder toBuilder() {
    Builder b = new Builder();
    b.from = from;
    b.to = to;
    b.subject = subject;
    b.emailBody = emailBody;
    b.replyTo = replyTo;
    b.userReferenceId = userReferenceId;
    b.callbackUrl = callbackUrl;
    b.source = source;
    return b;
  }

  /**
   * The registered sender address.
   *
   * @return the sender
   */
  public String from() {
    return from;
  }

  /**
   * The recipients (a single recipient given as a string is a one-element list here, and is sent as
   * a string).
   *
   * @return the recipients
   */
  public List<String> to() {
    return to.asList();
  }

  /**
   * The subject.
   *
   * @return the subject
   */
  public String subject() {
    return subject;
  }

  /**
   * The HTML body.
   *
   * @return the body
   */
  public String emailBody() {
    return emailBody;
  }

  /**
   * The reply-to address, or {@code null}.
   *
   * @return the address, or {@code null}
   */
  public String replyTo() {
    return replyTo;
  }

  /**
   * Your reference, or {@code null}. It lets the gateway refuse a later repeat of the same send,
   * but it does not make an email send retry-safe.
   *
   * @return the reference, or {@code null}
   */
  public String userReferenceId() {
    return userReferenceId;
  }

  /**
   * Where delivery reports are posted, or {@code null}.
   *
   * @return the URL, or {@code null}
   */
  public String callbackUrl() {
    return callbackUrl;
  }

  /**
   * A label for your own reporting, or {@code null}.
   *
   * @return the label, or {@code null}
   */
  public String source() {
    return source;
  }

  /**
   * The JSON this sends, with the API's field names.
   *
   * @return a new JSON object
   */
  public JsonObject toJson() {
    JsonObject o = new JsonObject();
    o.addProperty("from", from);
    o.add("to", to.toJson());
    o.addProperty("subject", subject);
    o.addProperty("email_body", emailBody);
    if (replyTo != null) {
      o.addProperty("reply_to", replyTo);
    }
    if (userReferenceId != null) {
      o.addProperty("user_reference_id", userReferenceId);
    }
    if (callbackUrl != null) {
      o.addProperty("callback_url", callbackUrl);
    }
    if (source != null) {
      o.addProperty("source", source);
    }
    return o;
  }

  /**
   * Builds {@link EmailSendParams}. {@code from}, {@code to}, {@code subject} and {@code emailBody}
   * are required.
   */
  public static final class Builder {
    private String from;
    private Recipients to;
    private String subject;
    private String emailBody;
    private String replyTo;
    private String userReferenceId;
    private String callbackUrl;
    private String source;

    private Builder() {}

    /**
     * The registered sender address (required).
     *
     * @param from the sender
     * @return this builder
     */
    public Builder from(String from) {
      this.from = from;
      return this;
    }

    /**
     * One recipient, sent as a string (required, or {@link #to(List)}).
     *
     * @param to the address
     * @return this builder
     */
    public Builder to(String to) {
      this.to = Recipients.one(to);
      return this;
    }

    /**
     * The recipients, sent as a list (required, or {@link #to(String)}): 1 to 2,000 addresses.
     *
     * @param to the addresses
     * @return this builder
     */
    public Builder to(List<String> to) {
      this.to = Recipients.many(to);
      return this;
    }

    /**
     * The subject (required).
     *
     * @param subject the subject
     * @return this builder
     */
    public Builder subject(String subject) {
      this.subject = subject;
      return this;
    }

    /**
     * The HTML body (required).
     *
     * @param emailBody the body
     * @return this builder
     */
    public Builder emailBody(String emailBody) {
      this.emailBody = emailBody;
      return this;
    }

    /**
     * The reply-to address (optional).
     *
     * @param replyTo the address, or {@code null} for none
     * @return this builder
     */
    public Builder replyTo(String replyTo) {
      this.replyTo = replyTo;
      return this;
    }

    /**
     * Your reference (optional).
     *
     * @param userReferenceId the reference, or {@code null} for none
     * @return this builder
     */
    public Builder userReferenceId(String userReferenceId) {
      this.userReferenceId = userReferenceId;
      return this;
    }

    /**
     * Where delivery reports are posted (optional).
     *
     * @param callbackUrl the URL, or {@code null} for none
     * @return this builder
     */
    public Builder callbackUrl(String callbackUrl) {
      this.callbackUrl = callbackUrl;
      return this;
    }

    /**
     * A label for your own reporting (optional).
     *
     * @param source the label, or {@code null} for none
     * @return this builder
     */
    public Builder source(String source) {
      this.source = source;
      return this;
    }

    /**
     * Builds the parameters.
     *
     * @return the parameters
     * @throws NullPointerException when a required field is not set
     */
    public EmailSendParams build() {
      Objects.requireNonNull(from, "from is required");
      Objects.requireNonNull(to, "to is required");
      Objects.requireNonNull(subject, "subject is required");
      Objects.requireNonNull(emailBody, "emailBody is required");
      return new EmailSendParams(this);
    }
  }
}
