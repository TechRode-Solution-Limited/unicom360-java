package com.unicom360.params;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The parameters of {@code client.whatsapp().sendTemplate(...)}: one approved template to 1 to
 * 1,000 recipients, each with its own parameters.
 *
 * <pre>{@code
 * TemplateSendParams params = TemplateSendParams.builder()
 *     .template("order_ready")
 *     .language("en_US")
 *     .addRecipient(TemplateRecipient.builder()
 *         .to("260971234567")
 *         .userReferenceId("inv-42")
 *         .body(TemplateBody.of("Ada", "ZM-4821"))
 *         .build())
 *     .build();
 * }</pre>
 */
public final class TemplateSendParams {
  private final String template;
  private final String from;
  private final String language;
  private final String callbackUrl;
  private final List<TemplateRecipient> recipients;

  private TemplateSendParams(Builder b) {
    this.template = b.template;
    this.from = b.from;
    this.language = b.language;
    this.callbackUrl = b.callbackUrl;
    this.recipients = List.copyOf(b.recipients);
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
    b.template = template;
    b.from = from;
    b.language = language;
    b.callbackUrl = callbackUrl;
    b.recipients = new ArrayList<>(recipients);
    return b;
  }

  /**
   * The template's name.
   *
   * @return the name
   */
  public String template() {
    return template;
  }

  /**
   * Your WhatsApp sender, or {@code null} (optional with exactly one approved sender).
   *
   * @return the sender, or {@code null}
   */
  public String from() {
    return from;
  }

  /**
   * The template's language code, or {@code null}.
   *
   * @return the language, or {@code null}
   */
  public String language() {
    return language;
  }

  /**
   * Where status callbacks are posted, or {@code null}.
   *
   * @return the URL, or {@code null}
   */
  public String callbackUrl() {
    return callbackUrl;
  }

  /**
   * The recipients, in order.
   *
   * @return the recipients
   */
  public List<TemplateRecipient> recipients() {
    return recipients;
  }

  /**
   * The JSON this sends, with the API's field names.
   *
   * @return a new JSON object
   */
  public JsonObject toJson() {
    JsonObject o = new JsonObject();
    o.addProperty("template", template);
    if (from != null) {
      o.addProperty("from", from);
    }
    if (language != null) {
      o.addProperty("language", language);
    }
    if (callbackUrl != null) {
      o.addProperty("callback_url", callbackUrl);
    }
    JsonArray array = new JsonArray();
    recipients.forEach(recipient -> array.add(recipient.toJson()));
    o.add("recipients", array);
    return o;
  }

  /** Builds {@link TemplateSendParams}. {@code template} and the recipients are required. */
  public static final class Builder {
    private String template;
    private String from;
    private String language;
    private String callbackUrl;
    private List<TemplateRecipient> recipients = new ArrayList<>();

    private Builder() {}

    /**
     * The template's name (required).
     *
     * @param template the name
     * @return this builder
     */
    public Builder template(String template) {
      this.template = template;
      return this;
    }

    /**
     * Your WhatsApp sender (optional with exactly one approved sender).
     *
     * @param from the sender, or {@code null} for none
     * @return this builder
     */
    public Builder from(String from) {
      this.from = from;
      return this;
    }

    /**
     * The template's language code (optional).
     *
     * @param language the language, or {@code null} for none
     * @return this builder
     */
    public Builder language(String language) {
      this.language = language;
      return this;
    }

    /**
     * Where status callbacks are posted (optional).
     *
     * @param callbackUrl the URL, or {@code null} for none
     * @return this builder
     */
    public Builder callbackUrl(String callbackUrl) {
      this.callbackUrl = callbackUrl;
      return this;
    }

    /**
     * The recipients, replacing any added before.
     *
     * @param recipients the recipients, in order
     * @return this builder
     */
    public Builder recipients(List<TemplateRecipient> recipients) {
      this.recipients = new ArrayList<>(recipients);
      return this;
    }

    /**
     * Adds a recipient.
     *
     * @param recipient the recipient
     * @return this builder
     */
    public Builder addRecipient(TemplateRecipient recipient) {
      recipients.add(Objects.requireNonNull(recipient, "recipient"));
      return this;
    }

    /**
     * Builds the parameters.
     *
     * @return the parameters
     * @throws NullPointerException when {@code template} is not set
     */
    public TemplateSendParams build() {
      Objects.requireNonNull(template, "template is required");
      return new TemplateSendParams(this);
    }
  }
}
