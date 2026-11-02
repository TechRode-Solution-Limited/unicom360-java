package com.unicom360.params;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One recipient of a template send, with its own parameters.
 *
 * <pre>{@code
 * TemplateRecipient recipient = TemplateRecipient.builder()
 *     .to("260971234567")
 *     .userReferenceId("inv-42")
 *     .header(TemplateHeader.image("https://example.com/receipts/ZM-4821.png"))
 *     .body(TemplateBody.of("Ada", "ZM-4821"))
 *     .addButton(TemplateButton.url(0, "ZM-4821"))
 *     .build();
 * }</pre>
 */
public final class TemplateRecipient {
  private final String to;
  private final String userReferenceId;
  private final TemplateHeader header;
  private final TemplateBody body;
  private final List<TemplateButton> buttons;
  private final String otp;

  private TemplateRecipient(Builder b) {
    this.to = b.to;
    this.userReferenceId = b.userReferenceId;
    this.header = b.header;
    this.body = b.body;
    this.buttons = b.buttons == null ? null : List.copyOf(b.buttons);
    this.otp = b.otp;
  }

  /**
   * A recipient without parameters.
   *
   * @param to the number, international form
   * @return the recipient
   */
  public static TemplateRecipient of(String to) {
    return builder().to(to).build();
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
   * A builder holding this recipient.
   *
   * @return the builder
   */
  public Builder toBuilder() {
    Builder b = new Builder();
    b.to = to;
    b.userReferenceId = userReferenceId;
    b.header = header;
    b.body = body;
    b.buttons = buttons == null ? null : new ArrayList<>(buttons);
    b.otp = otp;
    return b;
  }

  /**
   * The number.
   *
   * @return the number
   */
  public String to() {
    return to;
  }

  /**
   * The recipient's reference, or {@code null}. A template send is retry-safe only when every
   * recipient has one.
   *
   * @return the reference, or {@code null}
   */
  public String userReferenceId() {
    return userReferenceId;
  }

  /**
   * The header parameter, or {@code null}.
   *
   * @return the header, or {@code null}
   */
  public TemplateHeader header() {
    return header;
  }

  /**
   * The body parameters, or {@code null}.
   *
   * @return the body, or {@code null}
   */
  public TemplateBody body() {
    return body;
  }

  /**
   * The button parameters ({@code null} when not set).
   *
   * @return the buttons, or {@code null}
   */
  public List<TemplateButton> buttons() {
    return buttons;
  }

  /**
   * The code of an {@code AUTHENTICATION} template, or {@code null}.
   *
   * @return the code, or {@code null}
   */
  public String otp() {
    return otp;
  }

  /**
   * The JSON this sends, with the API's field names.
   *
   * @return a new JSON object
   */
  public JsonObject toJson() {
    JsonObject o = new JsonObject();
    o.addProperty("to", to);
    if (userReferenceId != null) {
      o.addProperty("user_reference_id", userReferenceId);
    }
    if (header != null) {
      o.add("header", header.toJson());
    }
    if (body != null) {
      o.add("body", body.toJson());
    }
    if (buttons != null) {
      JsonArray array = new JsonArray();
      buttons.forEach(button -> array.add(button.toJson()));
      o.add("buttons", array);
    }
    if (otp != null) {
      o.addProperty("otp", otp);
    }
    return o;
  }

  /** Builds a {@link TemplateRecipient}. {@code to} is required. */
  public static final class Builder {
    private String to;
    private String userReferenceId;
    private TemplateHeader header;
    private TemplateBody body;
    private List<TemplateButton> buttons;
    private String otp;

    private Builder() {}

    /**
     * The number (required), international form.
     *
     * @param to the number
     * @return this builder
     */
    public Builder to(String to) {
      this.to = to;
      return this;
    }

    /**
     * The recipient's reference (optional).
     *
     * @param userReferenceId the reference, or {@code null} for none
     * @return this builder
     */
    public Builder userReferenceId(String userReferenceId) {
      this.userReferenceId = userReferenceId;
      return this;
    }

    /**
     * The header parameter (optional).
     *
     * @param header the header, or {@code null} for none
     * @return this builder
     */
    public Builder header(TemplateHeader header) {
      this.header = header;
      return this;
    }

    /**
     * The body parameters (optional).
     *
     * @param body the body, or {@code null} for none
     * @return this builder
     */
    public Builder body(TemplateBody body) {
      this.body = body;
      return this;
    }

    /**
     * The button parameters (optional), replacing any added before.
     *
     * @param buttons the buttons, or {@code null} for none
     * @return this builder
     */
    public Builder buttons(List<TemplateButton> buttons) {
      this.buttons = buttons == null ? null : new ArrayList<>(buttons);
      return this;
    }

    /**
     * Adds a button parameter.
     *
     * @param button the button
     * @return this builder
     */
    public Builder addButton(TemplateButton button) {
      if (buttons == null) {
        buttons = new ArrayList<>();
      }
      buttons.add(Objects.requireNonNull(button, "button"));
      return this;
    }

    /**
     * The code of an {@code AUTHENTICATION} template (optional).
     *
     * @param otp the code, or {@code null} for none
     * @return this builder
     */
    public Builder otp(String otp) {
      this.otp = otp;
      return this;
    }

    /**
     * Builds the recipient.
     *
     * @return the recipient
     * @throws NullPointerException when {@code to} is not set
     */
    public TemplateRecipient build() {
      Objects.requireNonNull(to, "to is required");
      return new TemplateRecipient(this);
    }
  }
}
