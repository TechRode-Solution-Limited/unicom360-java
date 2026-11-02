package com.unicom360.params;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A contact card (Meta's contact object), for {@link SessionMessage#contacts}.
 *
 * <pre>{@code
 * ContactCard card = ContactCard.builder()
 *     .formattedName("Unicom360 Support")
 *     .firstName("Unicom360")
 *     .lastName("Support")
 *     .phone("+260960123456", "WORK", "260960123456")
 *     .email("info@techrode.co", "WORK")
 *     .url("https://unicom360.com", "WORK")
 *     .org("Techrode Solutions Limited")
 *     .build();
 * }</pre>
 */
public final class ContactCard {
  private final String formattedName;
  private final String firstName;
  private final String lastName;
  private final String middleName;
  private final String suffix;
  private final String prefix;
  private final List<Phone> phones;
  private final List<Email> emails;
  private final List<Url> urls;
  private final List<Address> addresses;
  private final Org org;
  private final String birthday;

  private ContactCard(Builder b) {
    this.formattedName = b.formattedName;
    this.firstName = b.firstName;
    this.lastName = b.lastName;
    this.middleName = b.middleName;
    this.suffix = b.suffix;
    this.prefix = b.prefix;
    this.phones = List.copyOf(b.phones);
    this.emails = List.copyOf(b.emails);
    this.urls = List.copyOf(b.urls);
    this.addresses = List.copyOf(b.addresses);
    this.org = b.org;
    this.birthday = b.birthday;
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
   * The JSON this sends, in Meta's shape.
   *
   * @return a new JSON object
   */
  public JsonObject toJson() {
    JsonObject name = new JsonObject();
    name.addProperty("formatted_name", formattedName);
    put(name, "first_name", firstName);
    put(name, "last_name", lastName);
    put(name, "middle_name", middleName);
    put(name, "suffix", suffix);
    put(name, "prefix", prefix);
    JsonObject o = new JsonObject();
    o.add("name", name);
    if (!phones.isEmpty()) {
      JsonArray array = new JsonArray();
      phones.forEach(phone -> array.add(phone.toJson()));
      o.add("phones", array);
    }
    if (!emails.isEmpty()) {
      JsonArray array = new JsonArray();
      emails.forEach(email -> array.add(email.toJson()));
      o.add("emails", array);
    }
    if (!urls.isEmpty()) {
      JsonArray array = new JsonArray();
      urls.forEach(url -> array.add(url.toJson()));
      o.add("urls", array);
    }
    if (!addresses.isEmpty()) {
      JsonArray array = new JsonArray();
      addresses.forEach(address -> array.add(address.toJson()));
      o.add("addresses", array);
    }
    if (org != null) {
      o.add("org", org.toJson());
    }
    put(o, "birthday", birthday);
    return o;
  }

  private static void put(JsonObject o, String key, String value) {
    if (value != null) {
      o.addProperty(key, value);
    }
  }

  /**
   * A phone number on a card.
   *
   * @param phone the number
   * @param type for example {@code WORK} or {@code CELL}, or {@code null}
   * @param waId the WhatsApp id, or {@code null}
   */
  public record Phone(String phone, String type, String waId) {
    /**
     * Requires a number.
     *
     * @param phone the number
     * @param type for example {@code WORK} or {@code CELL}, or {@code null}
     * @param waId the WhatsApp id, or {@code null}
     */
    public Phone {
      Objects.requireNonNull(phone, "phone");
    }

    JsonObject toJson() {
      JsonObject o = new JsonObject();
      o.addProperty("phone", phone);
      put(o, "type", type);
      put(o, "wa_id", waId);
      return o;
    }
  }

  /**
   * An email address on a card.
   *
   * @param email the address
   * @param type for example {@code WORK}, or {@code null}
   */
  public record Email(String email, String type) {
    /**
     * Requires an address.
     *
     * @param email the address
     * @param type for example {@code WORK}, or {@code null}
     */
    public Email {
      Objects.requireNonNull(email, "email");
    }

    JsonObject toJson() {
      JsonObject o = new JsonObject();
      o.addProperty("email", email);
      put(o, "type", type);
      return o;
    }
  }

  /**
   * A URL on a card.
   *
   * @param url the URL
   * @param type for example {@code WORK}, or {@code null}
   */
  public record Url(String url, String type) {
    /**
     * Requires a URL.
     *
     * @param url the URL
     * @param type for example {@code WORK}, or {@code null}
     */
    public Url {
      Objects.requireNonNull(url, "url");
    }

    JsonObject toJson() {
      JsonObject o = new JsonObject();
      o.addProperty("url", url);
      put(o, "type", type);
      return o;
    }
  }

  /**
   * A postal address on a card. Every part is optional.
   *
   * @param street the street, or {@code null}
   * @param city the city, or {@code null}
   * @param state the state, or {@code null}
   * @param zip the postal code, or {@code null}
   * @param country the country, or {@code null}
   * @param countryCode the country code, or {@code null}
   * @param type for example {@code WORK}, or {@code null}
   */
  public record Address(
      String street,
      String city,
      String state,
      String zip,
      String country,
      String countryCode,
      String type) {
    JsonObject toJson() {
      JsonObject o = new JsonObject();
      put(o, "street", street);
      put(o, "city", city);
      put(o, "state", state);
      put(o, "zip", zip);
      put(o, "country", country);
      put(o, "country_code", countryCode);
      put(o, "type", type);
      return o;
    }
  }

  /**
   * An organisation on a card. Every part is optional.
   *
   * @param company the company, or {@code null}
   * @param department the department, or {@code null}
   * @param title the job title, or {@code null}
   */
  public record Org(String company, String department, String title) {
    JsonObject toJson() {
      JsonObject o = new JsonObject();
      put(o, "company", company);
      put(o, "department", department);
      put(o, "title", title);
      return o;
    }
  }

  /** Builds a {@link ContactCard}. {@code formattedName} is required. */
  public static final class Builder {
    private String formattedName;
    private String firstName;
    private String lastName;
    private String middleName;
    private String suffix;
    private String prefix;
    private final List<Phone> phones = new ArrayList<>();
    private final List<Email> emails = new ArrayList<>();
    private final List<Url> urls = new ArrayList<>();
    private final List<Address> addresses = new ArrayList<>();
    private Org org;
    private String birthday;

    private Builder() {}

    /**
     * The full name as shown (required).
     *
     * @param formattedName the name
     * @return this builder
     */
    public Builder formattedName(String formattedName) {
      this.formattedName = formattedName;
      return this;
    }

    /**
     * The first name (optional).
     *
     * @param firstName the first name, or {@code null} for none
     * @return this builder
     */
    public Builder firstName(String firstName) {
      this.firstName = firstName;
      return this;
    }

    /**
     * The last name (optional).
     *
     * @param lastName the last name, or {@code null} for none
     * @return this builder
     */
    public Builder lastName(String lastName) {
      this.lastName = lastName;
      return this;
    }

    /**
     * The middle name (optional).
     *
     * @param middleName the middle name, or {@code null} for none
     * @return this builder
     */
    public Builder middleName(String middleName) {
      this.middleName = middleName;
      return this;
    }

    /**
     * The name suffix (optional).
     *
     * @param suffix the suffix, or {@code null} for none
     * @return this builder
     */
    public Builder suffix(String suffix) {
      this.suffix = suffix;
      return this;
    }

    /**
     * The name prefix (optional).
     *
     * @param prefix the prefix, or {@code null} for none
     * @return this builder
     */
    public Builder prefix(String prefix) {
      this.prefix = prefix;
      return this;
    }

    /**
     * Adds a phone number. The gateway requires at least one.
     *
     * @param phone the number
     * @return this builder
     */
    public Builder phone(String phone) {
      return phone(phone, null, null);
    }

    /**
     * Adds a phone number with a type.
     *
     * @param phone the number
     * @param type for example {@code WORK} or {@code CELL}, or {@code null}
     * @return this builder
     */
    public Builder phone(String phone, String type) {
      return phone(phone, type, null);
    }

    /**
     * Adds a phone number with a type and a WhatsApp id.
     *
     * @param phone the number
     * @param type for example {@code WORK} or {@code CELL}, or {@code null}
     * @param waId the WhatsApp id, or {@code null}
     * @return this builder
     */
    public Builder phone(String phone, String type, String waId) {
      phones.add(new Phone(phone, type, waId));
      return this;
    }

    /**
     * Adds an email address.
     *
     * @param email the address
     * @return this builder
     */
    public Builder email(String email) {
      return email(email, null);
    }

    /**
     * Adds an email address with a type.
     *
     * @param email the address
     * @param type for example {@code WORK}, or {@code null}
     * @return this builder
     */
    public Builder email(String email, String type) {
      emails.add(new Email(email, type));
      return this;
    }

    /**
     * Adds a URL.
     *
     * @param url the URL
     * @return this builder
     */
    public Builder url(String url) {
      return url(url, null);
    }

    /**
     * Adds a URL with a type.
     *
     * @param url the URL
     * @param type for example {@code WORK}, or {@code null}
     * @return this builder
     */
    public Builder url(String url, String type) {
      urls.add(new Url(url, type));
      return this;
    }

    /**
     * Adds a postal address.
     *
     * @param address the address
     * @return this builder
     */
    public Builder address(Address address) {
      addresses.add(Objects.requireNonNull(address, "address"));
      return this;
    }

    /**
     * The organisation's company (optional).
     *
     * @param company the company
     * @return this builder
     */
    public Builder org(String company) {
      return org(company, null, null);
    }

    /**
     * The organisation (optional).
     *
     * @param company the company, or {@code null}
     * @param department the department, or {@code null}
     * @param title the job title, or {@code null}
     * @return this builder
     */
    public Builder org(String company, String department, String title) {
      this.org = new Org(company, department, title);
      return this;
    }

    /**
     * The birthday (optional), {@code YYYY-MM-DD}.
     *
     * @param birthday the birthday, or {@code null} for none
     * @return this builder
     */
    public Builder birthday(String birthday) {
      this.birthday = birthday;
      return this;
    }

    /**
     * Builds the card.
     *
     * @return the card
     * @throws NullPointerException when {@code formattedName} is not set
     */
    public ContactCard build() {
      Objects.requireNonNull(formattedName, "formattedName is required");
      return new ContactCard(this);
    }
  }
}
