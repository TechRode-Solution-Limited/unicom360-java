package com.unicom360.params;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;

/**
 * The parameters of {@code client.sms().send(...)}.
 *
 * <pre>{@code
 * SmsSendParams params = SmsSendParams.builder()
 *     .from("Unicom360")
 *     .to(List.of("260971234567", "260961234567"))
 *     .message("Your verification code is 482193.")
 *     .userReferenceId("otp-7781") // optional: makes the send safe to retry
 *     .build();
 * }</pre>
 */
public final class SmsSendParams {
  private final String from;
  private final Recipients to;
  private final String message;
  private final String userReferenceId;
  private final String callbackUrl;
  private final String source;

  private SmsSendParams(Builder b) {
    this.from = b.from;
    this.to = b.to;
    this.message = b.message;
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
    b.message = message;
    b.userReferenceId = userReferenceId;
    b.callbackUrl = callbackUrl;
    b.source = source;
    return b;
  }

  /**
   * The registered sender ID.
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
   * The text: GSM-7, at most 1,200 characters (see {@link com.unicom360.Gsm}).
   *
   * @return the text
   */
  public String message() {
    return message;
  }

  /**
   * Your reference, or {@code null}. With one, the gateway refuses a repeat for the same recipient
   * and sender, which makes the send safe to retry.
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
    o.addProperty("message", message);
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

  /** Builds {@link SmsSendParams}. {@code from}, {@code to} and {@code message} are required. */
  public static final class Builder {
    private String from;
    private Recipients to;
    private String message;
    private String userReferenceId;
    private String callbackUrl;
    private String source;

    private Builder() {}

    /**
     * The registered sender ID (required).
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
     * @param to the number, international form ({@code 260971234567})
     * @return this builder
     */
    public Builder to(String to) {
      this.to = Recipients.one(to);
      return this;
    }

    /**
     * The recipients, sent as a list (required, or {@link #to(String)}): 1 to 2,000 numbers.
     *
     * @param to the numbers, international form
     * @return this builder
     */
    public Builder to(List<String> to) {
      this.to = Recipients.many(to);
      return this;
    }

    /**
     * The text (required): GSM-7, at most 1,200 characters.
     *
     * @param message the text
     * @return this builder
     */
    public Builder message(String message) {
      this.message = message;
      return this;
    }

    /**
     * Your reference (optional); makes the send safe to retry.
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
     * @throws NullPointerException when {@code from}, {@code to} or {@code message} is not set
     */
    public SmsSendParams build() {
      Objects.requireNonNull(from, "from is required");
      Objects.requireNonNull(to, "to is required");
      Objects.requireNonNull(message, "message is required");
      return new SmsSendParams(this);
    }
  }
}
