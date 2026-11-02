package com.unicom360.conformance;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.unicom360.Unicom360Client;
import com.unicom360.errors.Unicom360Exception;
import com.unicom360.params.ContactCard;
import com.unicom360.params.ContentMessage;
import com.unicom360.params.EmailSendParams;
import com.unicom360.params.ListRow;
import com.unicom360.params.ListSection;
import com.unicom360.params.ReplyButton;
import com.unicom360.params.SessionMessage;
import com.unicom360.params.SmsSendParams;
import com.unicom360.params.TemplateBody;
import com.unicom360.params.TemplateButton;
import com.unicom360.params.TemplateHeader;
import com.unicom360.params.TemplateRecipient;
import com.unicom360.params.TemplateSendParams;
import com.unicom360.transport.Transport;
import com.unicom360.transport.TransportException;
import com.unicom360.transport.TransportRequest;
import com.unicom360.transport.TransportResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.DoubleSupplier;

/**
 * The conformance harness: fixture loading, fakes, the operation mapping and canonical form.
 *
 * <p>The fixtures under {@code src/test/resources/conformance} are vendored from {@code
 * unicom360-openapi} (see its {@code conformance/README.md}). Every vector of every file runs
 * through the public API; a vector or key this harness does not understand fails rather than being
 * skipped.
 */
final class Harness {
  static final String API_KEY = "test-api-key";
  static final Path FIXTURES =
      Path.of(System.getProperty("basedir", "."), "src", "test", "resources", "conformance");

  private Harness() {}

  // -- fixtures ------------------------------------------------------------------------------

  static JsonObject load(String name) {
    try {
      String text = Files.readString(FIXTURES.resolve(name), StandardCharsets.UTF_8);
      return JsonParser.parseString(text).getAsJsonObject();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static List<JsonObject> vectors(JsonObject file) {
    List<JsonObject> out = new ArrayList<>();
    for (JsonElement vector : file.getAsJsonArray("vectors")) {
      out.add(vector.getAsJsonObject());
    }
    return out;
  }

  static void checkKeys(JsonObject object, Set<String> required, Set<String> optional) {
    String id = object.has("id") ? object.get("id").getAsString() : object.toString();
    Set<String> keys = object.keySet();
    for (String key : required) {
      assertTrue(keys.contains(key), id + ": missing key " + key);
    }
    for (String key : keys) {
      assertTrue(
          required.contains(key) || optional.contains(key),
          id + ": a key this harness does not understand: " + key);
    }
  }

  /** Reads a params object and fails if any key is left unread. */
  static final class Params {
    private final String what;
    private final JsonObject params;
    private final Set<String> read = new HashSet<>();

    Params(String what, JsonElement params) {
      this.what = what;
      this.params = params.getAsJsonObject();
    }

    JsonElement req(String key) {
      read.add(key);
      JsonElement value = params.get(key);
      if (value == null || value.isJsonNull()) {
        fail(what + ": missing " + key);
      }
      return value;
    }

    JsonElement opt(String key) {
      read.add(key);
      JsonElement value = params.get(key);
      return value == null || value.isJsonNull() ? null : value;
    }

    String reqString(String key) {
      return string(key, req(key));
    }

    String optString(String key) {
      JsonElement value = opt(key);
      return value == null ? null : string(key, value);
    }

    private String string(String key, JsonElement value) {
      if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
        fail(what + ": " + key + " is not a string: " + value);
      }
      return value.getAsString();
    }

    void done() {
      Set<String> unread = new TreeSet<>(params.keySet());
      unread.removeAll(read);
      assertTrue(unread.isEmpty(), what + ": params this harness does not understand: " + unread);
    }
  }

  static List<String> stringList(JsonElement array) {
    List<String> out = new ArrayList<>();
    for (JsonElement element : array.getAsJsonArray()) {
      out.add(element.getAsString());
    }
    return out;
  }

  // -- fakes ---------------------------------------------------------------------------------

  /** A logger that discards everything, to keep the test output readable. */
  static final class SilentLogger implements System.Logger {
    @Override
    public String getName() {
      return "silent";
    }

    @Override
    public boolean isLoggable(Level level) {
      return false;
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {}

    @Override
    public void log(Level level, ResourceBundle bundle, String format, Object... params) {}
  }

  static TransportResponse httpResponse(JsonObject http) {
    checkKeys(http, Set.of("status", "headers", "body_text"), Set.of());
    Map<String, List<String>> headers = new LinkedHashMap<>();
    for (Map.Entry<String, JsonElement> header : http.getAsJsonObject("headers").entrySet()) {
      headers.put(header.getKey(), List.of(header.getValue().getAsString()));
    }
    return new TransportResponse(
        http.get("status").getAsInt(), headers, http.get("body_text").getAsString());
  }

  /** Answers attempt n with {@code steps.get(n - 1)} and records every request. */
  static final class FakeTransport implements Transport {
    final List<Object> steps;
    final List<TransportRequest> requests = new ArrayList<>();

    FakeTransport(List<Object> steps) {
      this.steps = steps;
    }

    static FakeTransport answering(JsonObject http) {
      return new FakeTransport(List.of(httpResponse(http)));
    }

    @Override
    public TransportResponse send(TransportRequest request) throws TransportException {
      requests.add(request);
      int index = requests.size() - 1;
      if (index >= steps.size()) {
        return fail("unexpected attempt " + (index + 1) + ": no step for it");
      }
      Object step = steps.get(index);
      if (step instanceof TransportException failure) {
        throw failure;
      }
      return (TransportResponse) step;
    }
  }

  /** A clock that moves only when the sleeper sleeps, by exactly the time slept. */
  static final class FakeTime extends Clock {
    private Instant now;
    final List<Duration> sleeps = new ArrayList<>();

    FakeTime(Instant now) {
      this.now = now;
    }

    void sleep(Duration duration) {
      sleeps.add(duration);
      now = now.plus(duration);
    }

    @Override
    public Instant instant() {
      return now;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }
  }

  static Instant instant(JsonElement unixSeconds) {
    BigDecimal seconds = unixSeconds.getAsBigDecimal();
    long whole = seconds.setScale(0, java.math.RoundingMode.FLOOR).longValueExact();
    long nanos = seconds.subtract(BigDecimal.valueOf(whole)).movePointRight(9).longValueExact();
    return Instant.ofEpochSecond(whole, nanos);
  }

  static double secondsOf(Duration duration) {
    return duration.getSeconds() + duration.getNano() / 1e9;
  }

  static Unicom360Client client(
      Transport transport,
      FakeTime time,
      int maxRetries,
      Duration maxRetryWait,
      DoubleSupplier random) {
    return Unicom360Client.builder()
        .apiKey(API_KEY)
        .baseUrl(Unicom360Client.DEFAULT_BASE_URL)
        .maxRetries(maxRetries)
        .maxRetryWait(maxRetryWait)
        .transport(transport)
        .clock(time)
        .sleeper(time::sleep)
        .random(random)
        .logger(new SilentLogger())
        .build();
  }

  // -- operations: canonical params (API names) to SDK calls ---------------------------------

  static final List<String> OPERATIONS =
      List.of(
          "sms.send",
          "email.send",
          "whatsapp.send_template",
          "whatsapp.send_session",
          "whatsapp.status",
          "whatsapp.templates",
          "balance.get",
          "balance.all",
          "health");

  /** Calls {@code operation} with {@code params} (API field names) through the public API. */
  static Object call(Unicom360Client client, String operation, JsonObject params) {
    if (operation.equals("whatsapp.send_session")) {
      return client.whatsapp().sendSession(sessionMessage(params));
    }
    Params p = new Params(operation, params);
    Object result;
    switch (operation) {
      case "sms.send":
        {
          SmsSendParams.Builder b =
              SmsSendParams.builder().from(p.reqString("from")).message(p.reqString("message"));
          JsonElement to = p.req("to");
          if (to.isJsonArray()) {
            b.to(stringList(to));
          } else {
            b.to(to.getAsString());
          }
          result =
              client
                  .sms()
                  .send(
                      b.userReferenceId(p.optString("user_reference_id"))
                          .callbackUrl(p.optString("callback_url"))
                          .source(p.optString("source"))
                          .build());
          break;
        }
      case "email.send":
        {
          EmailSendParams.Builder b =
              EmailSendParams.builder()
                  .from(p.reqString("from"))
                  .subject(p.reqString("subject"))
                  .emailBody(p.reqString("email_body"));
          JsonElement to = p.req("to");
          if (to.isJsonArray()) {
            b.to(stringList(to));
          } else {
            b.to(to.getAsString());
          }
          result =
              client
                  .email()
                  .send(
                      b.replyTo(p.optString("reply_to"))
                          .userReferenceId(p.optString("user_reference_id"))
                          .callbackUrl(p.optString("callback_url"))
                          .source(p.optString("source"))
                          .build());
          break;
        }
      case "whatsapp.send_template":
        {
          List<TemplateRecipient> recipients = new ArrayList<>();
          for (JsonElement recipient : p.req("recipients").getAsJsonArray()) {
            recipients.add(templateRecipient(recipient));
          }
          result =
              client
                  .whatsapp()
                  .sendTemplate(
                      TemplateSendParams.builder()
                          .template(p.reqString("template"))
                          .from(p.optString("from"))
                          .language(p.optString("language"))
                          .callbackUrl(p.optString("callback_url"))
                          .recipients(recipients)
                          .build());
          break;
        }
      case "whatsapp.status":
        result = client.whatsapp().status(stringList(p.req("message_ids")));
        break;
      case "whatsapp.templates":
        result = client.whatsapp().templates();
        break;
      case "balance.get":
        {
          String bundleType = p.optString("bundle_type");
          result = bundleType == null ? client.balance().get() : client.balance().get(bundleType);
          break;
        }
      case "balance.all":
        result = client.balance().all();
        break;
      case "health":
        result = client.health();
        break;
      default:
        return fail("unknown operation " + operation);
    }
    p.done();
    return result;
  }

  private static TemplateRecipient templateRecipient(JsonElement value) {
    Params p = new Params("template recipient", value);
    TemplateRecipient.Builder b =
        TemplateRecipient.builder()
            .to(p.reqString("to"))
            .userReferenceId(p.optString("user_reference_id"))
            .otp(p.optString("otp"));
    JsonElement header = p.opt("header");
    if (header != null) {
      b.header(templateHeader(header));
    }
    JsonElement body = p.opt("body");
    if (body != null) {
      if (body.isJsonArray()) {
        b.body(TemplateBody.of(stringList(body)));
      } else {
        Map<String, String> named = new LinkedHashMap<>();
        body.getAsJsonObject()
            .entrySet()
            .forEach(e -> named.put(e.getKey(), e.getValue().getAsString()));
        b.body(TemplateBody.named(named));
      }
    }
    JsonElement buttons = p.opt("buttons");
    if (buttons != null) {
      for (JsonElement button : buttons.getAsJsonArray()) {
        Params bp = new Params("template button", button);
        int index = bp.req("index").getAsInt();
        String type = bp.reqString("type");
        String text = bp.reqString("value");
        bp.done();
        switch (type) {
          case "url":
            b.addButton(TemplateButton.url(index, text));
            break;
          case "quick_reply":
            b.addButton(TemplateButton.quickReply(index, text));
            break;
          default:
            fail("unknown template button type " + type);
        }
      }
    }
    p.done();
    return b.build();
  }

  private static TemplateHeader templateHeader(JsonElement value) {
    if (value.isJsonPrimitive()) {
      return TemplateHeader.text(value.getAsString());
    }
    Params p = new Params("template header", value);
    String type = p.reqString("type");
    String url = p.reqString("url");
    TemplateHeader header;
    switch (type) {
      case "image":
        header = TemplateHeader.image(url);
        break;
      case "video":
        header = TemplateHeader.video(url);
        break;
      case "document":
        String filename = p.optString("filename");
        header =
            filename == null
                ? TemplateHeader.document(url)
                : TemplateHeader.document(url, filename);
        break;
      default:
        return fail("unknown template header type " + type);
    }
    p.done();
    return header;
  }

  /** Builds a session message with the factory for {@code params.type}. */
  static SessionMessage sessionMessage(JsonObject params) {
    Params p = new Params("session message", params);
    String to = p.reqString("to");
    String type = p.reqString("type");
    String from = p.optString("from");
    String reference = p.optString("user_reference_id");
    String callback = p.optString("callback_url");
    String replyTo = p.optString("reply_to");

    SessionMessage message;
    if (type.equals("reaction")) {
      assertTrue(replyTo == null, "a reaction takes no reply_to");
      Params r = new Params("reaction", p.req("reaction"));
      message =
          SessionMessage.reaction(to, r.reqString("message_id"), r.reqString("emoji"))
              .from(from)
              .userReferenceId(reference)
              .callbackUrl(callback);
      r.done();
    } else {
      ContentMessage content;
      switch (type) {
        case "text":
          content = SessionMessage.text(to, p.reqString("text"));
          break;
        case "image":
        case "video":
        case "document":
        case "audio":
        case "sticker":
          {
            Params m = new Params(type + " media", p.req("media"));
            String url = m.reqString("url");
            switch (type) {
              case "image":
                {
                  String caption = m.optString("caption");
                  content =
                      caption == null
                          ? SessionMessage.image(to, url)
                          : SessionMessage.image(to, url, caption);
                  break;
                }
              case "video":
                {
                  String caption = m.optString("caption");
                  content =
                      caption == null
                          ? SessionMessage.video(to, url)
                          : SessionMessage.video(to, url, caption);
                  break;
                }
              case "document":
                content =
                    SessionMessage.document(
                        to, url, m.optString("caption"), m.optString("filename"));
                break;
              case "audio":
                content = SessionMessage.audio(to, url);
                break;
              default:
                content = SessionMessage.sticker(to, url);
            }
            m.done();
            break;
          }
        case "interactive":
          content = interactive(to, new Params("interactive", p.req("interactive")));
          break;
        case "location":
          {
            Params l = new Params("location", p.req("location"));
            double latitude = l.req("latitude").getAsDouble();
            double longitude = l.req("longitude").getAsDouble();
            String name = l.optString("name");
            String address = l.optString("address");
            content =
                name == null && address == null
                    ? SessionMessage.location(to, latitude, longitude)
                    : SessionMessage.location(to, latitude, longitude, name, address);
            l.done();
            break;
          }
        case "contacts":
          {
            List<ContactCard> cards = new ArrayList<>();
            for (JsonElement card : p.req("contacts").getAsJsonArray()) {
              cards.add(contactCard(card));
            }
            content = SessionMessage.contacts(to, cards);
            break;
          }
        default:
          return fail("unknown session message type " + type);
      }
      message =
          content.from(from).replyTo(replyTo).userReferenceId(reference).callbackUrl(callback);
    }
    p.done();
    return message;
  }

  private static ContentMessage interactive(String to, Params i) {
    String type = i.reqString("type");
    String body = i.reqString("body");
    String header = i.optString("header");
    String footer = i.optString("footer");
    ContentMessage message;
    if (type.equals("button")) {
      List<ReplyButton> buttons = new ArrayList<>();
      for (JsonElement button : i.req("buttons").getAsJsonArray()) {
        Params b = new Params("reply button", button);
        buttons.add(ReplyButton.of(b.reqString("id"), b.reqString("title")));
        b.done();
      }
      message =
          header == null && footer == null
              ? SessionMessage.buttons(to, body, buttons)
              : SessionMessage.buttons(to, body, buttons, header, footer);
    } else if (type.equals("list")) {
      String button = i.reqString("button");
      List<ListSection> sections = new ArrayList<>();
      for (JsonElement section : i.req("sections").getAsJsonArray()) {
        Params s = new Params("list section", section);
        List<ListRow> rows = new ArrayList<>();
        for (JsonElement row : s.req("rows").getAsJsonArray()) {
          Params r = new Params("list row", row);
          String description = r.optString("description");
          rows.add(
              description == null
                  ? ListRow.of(r.reqString("id"), r.reqString("title"))
                  : ListRow.of(r.reqString("id"), r.reqString("title"), description));
          r.done();
        }
        String title = s.optString("title");
        sections.add(title == null ? ListSection.of(rows) : ListSection.of(title, rows));
        s.done();
      }
      message =
          header == null && footer == null
              ? SessionMessage.list(to, body, button, sections)
              : SessionMessage.list(to, body, button, sections, header, footer);
    } else {
      return fail("unknown interactive type " + type);
    }
    i.done();
    return message;
  }

  private static ContactCard contactCard(JsonElement value) {
    Params p = new Params("contact card", value);
    ContactCard.Builder b = ContactCard.builder();
    Params n = new Params("contact name", p.req("name"));
    b.formattedName(n.reqString("formatted_name"))
        .firstName(n.optString("first_name"))
        .lastName(n.optString("last_name"))
        .middleName(n.optString("middle_name"))
        .suffix(n.optString("suffix"))
        .prefix(n.optString("prefix"));
    n.done();
    JsonElement phones = p.opt("phones");
    if (phones != null) {
      for (JsonElement phone : phones.getAsJsonArray()) {
        Params q = new Params("contact phone", phone);
        b.phone(q.reqString("phone"), q.optString("type"), q.optString("wa_id"));
        q.done();
      }
    }
    JsonElement emails = p.opt("emails");
    if (emails != null) {
      for (JsonElement email : emails.getAsJsonArray()) {
        Params q = new Params("contact email", email);
        b.email(q.reqString("email"), q.optString("type"));
        q.done();
      }
    }
    JsonElement urls = p.opt("urls");
    if (urls != null) {
      for (JsonElement url : urls.getAsJsonArray()) {
        Params q = new Params("contact url", url);
        b.url(q.reqString("url"), q.optString("type"));
        q.done();
      }
    }
    JsonElement addresses = p.opt("addresses");
    if (addresses != null) {
      for (JsonElement address : addresses.getAsJsonArray()) {
        Params q = new Params("contact address", address);
        b.address(
            new ContactCard.Address(
                q.optString("street"),
                q.optString("city"),
                q.optString("state"),
                q.optString("zip"),
                q.optString("country"),
                q.optString("country_code"),
                q.optString("type")));
        q.done();
      }
    }
    JsonElement org = p.opt("org");
    if (org != null) {
      Params q = new Params("contact org", org);
      b.org(q.optString("company"), q.optString("department"), q.optString("title"));
      q.done();
    }
    b.birthday(p.optString("birthday"));
    p.done();
    return b.build();
  }

  /** Valid params for {@code operation}: those of its first {@code requests.json} vector. */
  static JsonObject sampleParams(String operation) {
    for (JsonObject vector : vectors(load("requests.json"))) {
      if (vector.get("operation").getAsString().equals(operation)) {
        return vector.getAsJsonObject("params");
      }
    }
    return fail("requests.json has no vector for " + operation);
  }

  // -- canonical form and comparison ---------------------------------------------------------

  private static final Map<String, String> RENAMED =
      Map.of("acceptedCount", "accepted", "rejectedCount", "rejected");

  private static String apiName(String component) {
    String renamed = RENAMED.get(component);
    if (renamed != null) {
      return renamed;
    }
    StringBuilder out = new StringBuilder();
    for (char c : component.toCharArray()) {
      if (Character.isUpperCase(c)) {
        out.append('_').append(Character.toLowerCase(c));
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }

  /**
   * The canonical JSON of a model: its typed fields under API names plus its {@code extra}. Built
   * from the typed model, never from {@code raw}.
   */
  @SuppressWarnings("unchecked")
  static JsonElement canonical(Object value) {
    if (value == null) {
      return JsonNull.INSTANCE;
    }
    if (value instanceof JsonElement element) {
      return element;
    }
    if (value instanceof String text) {
      return new JsonPrimitive(text);
    }
    if (value instanceof Number number) {
      return new JsonPrimitive(number);
    }
    if (value instanceof Boolean flag) {
      return new JsonPrimitive(flag);
    }
    if (value instanceof List<?> list) {
      JsonArray array = new JsonArray();
      list.forEach(item -> array.add(canonical(item)));
      return array;
    }
    if (value instanceof Map<?, ?> map) {
      JsonObject object = new JsonObject();
      map.forEach((k, v) -> object.add((String) k, canonical(v)));
      return object;
    }
    if (value.getClass().isRecord()) {
      JsonObject object = new JsonObject();
      Map<String, JsonElement> extra = Map.of();
      try {
        for (RecordComponent component : value.getClass().getRecordComponents()) {
          Object field = component.getAccessor().invoke(value);
          switch (component.getName()) {
            case "raw":
              break;
            case "extra":
              extra = (Map<String, JsonElement>) field;
              break;
            default:
              object.add(apiName(component.getName()), canonical(field));
          }
        }
      } catch (ReflectiveOperationException e) {
        throw new IllegalStateException(e);
      }
      for (Map.Entry<String, JsonElement> entry : extra.entrySet()) {
        assertTrue(
            !object.has(entry.getKey()), "extra field shadows a modelled field: " + entry.getKey());
        object.add(entry.getKey(), entry.getValue());
      }
      return object;
    }
    return fail("no canonical form for " + value.getClass());
  }

  static JsonElement dropNulls(JsonElement value) {
    if (value.isJsonObject()) {
      JsonObject out = new JsonObject();
      value
          .getAsJsonObject()
          .entrySet()
          .forEach(e -> {
            if (!e.getValue().isJsonNull()) {
              out.add(e.getKey(), dropNulls(e.getValue()));
            }
          });
      return out;
    }
    if (value.isJsonArray()) {
      JsonArray out = new JsonArray();
      value.getAsJsonArray().forEach(e -> out.add(dropNulls(e)));
      return out;
    }
    return value;
  }

  /** The first difference, or {@code null}. Numbers compare by value, key order is ignored. */
  static String difference(JsonElement a, JsonElement b, String path) {
    if (a.isJsonPrimitive()
        && a.getAsJsonPrimitive().isNumber()
        && b.isJsonPrimitive()
        && b.getAsJsonPrimitive().isNumber()) {
      return new BigDecimal(a.getAsString()).compareTo(new BigDecimal(b.getAsString())) == 0
          ? null
          : path + ": " + a + " != " + b;
    }
    if (a.isJsonArray() && b.isJsonArray()) {
      JsonArray x = a.getAsJsonArray();
      JsonArray y = b.getAsJsonArray();
      if (x.size() != y.size()) {
        return path + ": " + a + " != " + b;
      }
      for (int i = 0; i < x.size(); i++) {
        String problem = difference(x.get(i), y.get(i), path + "[" + i + "]");
        if (problem != null) {
          return problem;
        }
      }
      return null;
    }
    if (a.isJsonObject() && b.isJsonObject()) {
      JsonObject x = a.getAsJsonObject();
      JsonObject y = b.getAsJsonObject();
      if (!x.keySet().equals(y.keySet())) {
        return path + ": keys " + new TreeSet<>(x.keySet()) + " != " + new TreeSet<>(y.keySet());
      }
      for (String key : x.keySet()) {
        String problem = difference(x.get(key), y.get(key), path + "." + key);
        if (problem != null) {
          return problem;
        }
      }
      return null;
    }
    return a.equals(b) ? null : path + ": " + a + " != " + b;
  }

  static void assertCanonicalEqual(JsonElement expected, JsonElement actual) {
    String problem = difference(dropNulls(expected), dropNulls(actual), "$");
    assertTrue(problem == null, problem);
  }

  /** Whether the RFC 6901 {@code pointer} resolves in {@code document}. */
  static boolean pointerExists(JsonElement document, String pointer) {
    JsonElement current = document;
    String[] tokens = pointer.split("/", -1);
    for (int i = 1; i < tokens.length; i++) {
      String token = tokens[i].replace("~1", "/").replace("~0", "~");
      if (current.isJsonObject()) {
        if (!current.getAsJsonObject().has(token)) {
          return false;
        }
        current = current.getAsJsonObject().get(token);
      } else if (current.isJsonArray()) {
        if (!token.matches("[0-9]+")
            || Integer.parseInt(token) >= current.getAsJsonArray().size()) {
          return false;
        }
        current = current.getAsJsonArray().get(Integer.parseInt(token));
      } else {
        return false;
      }
    }
    return true;
  }

  /** The canonical class name of an exception: {@code ApiStatusException} is APIStatusError. */
  static String canonicalClass(Unicom360Exception error) {
    String name = error.getClass().getSimpleName().replaceFirst("^Api", "API");
    assertTrue(name.endsWith("Exception"), name);
    return name.substring(0, name.length() - "Exception".length()) + "Error";
  }

  /** Asserts an error's class is one the SDK's public package exports. */
  static void assertPublic(Unicom360Exception error) {
    assertTrue(
        error.getClass().getPackageName().equals("com.unicom360.errors"),
        error.getClass().getName());
  }
}
