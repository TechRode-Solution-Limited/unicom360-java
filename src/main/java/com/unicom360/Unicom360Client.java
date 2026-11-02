package com.unicom360;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.unicom360.errors.ApiConnectionException;
import com.unicom360.errors.ApiTimeoutException;
import com.unicom360.errors.ConfigurationException;
import com.unicom360.errors.Unicom360Exception;
import com.unicom360.internal.Interpreter;
import com.unicom360.internal.Json;
import com.unicom360.internal.Matching;
import com.unicom360.internal.Models;
import com.unicom360.internal.Operation;
import com.unicom360.models.Health;
import com.unicom360.transport.JdkHttpTransport;
import com.unicom360.transport.Transport;
import com.unicom360.transport.TransportException;
import com.unicom360.transport.TransportRequest;
import com.unicom360.transport.TransportResponse;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * The Unicom360 API client.
 *
 * <pre>{@code
 * Unicom360Client client = Unicom360Client.fromEnv(); // reads UNICOM360_API_KEY
 *
 * SmsSendResult result = client.sms().send(SmsSendParams.builder()
 *     .from("Unicom360")
 *     .to("260971234567")
 *     .message("Your order ZM4821 is ready.")
 *     .build());
 * }</pre>
 *
 * <p>The client is immutable and safe to share between threads. Use {@link #withOptions(Consumer)}
 * for a copy with other options. There is no sandbox: every accepted send is real and billed.
 *
 * <p>Operations throw the unchecked {@link Unicom360Exception} family; HTTP 200 is not success (a
 * refusal arrives as HTTP 200 with {@code success: false} and throws {@link
 * com.unicom360.errors.ApiException}). The client retries what is safe to retry, so a retry never
 * sends a message twice: see the README.
 */
public final class Unicom360Client {
  /** The default base URL. */
  public static final String DEFAULT_BASE_URL = "https://api.unicom360.com";

  private static final String LOGGER_NAME = "com.unicom360";
  private static final String REFERENCE_PREFIX = "u360-";
  private static final Pattern IPV4_LOOPBACK =
      Pattern.compile("127\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})");

  private final String apiKey;
  private final String baseUrl;
  private final Duration timeout;
  private final Duration connectTimeout;
  private final int maxRetries;
  private final Duration maxRetryWait;
  private final boolean autoReferenceIds;
  private final AppInfo appInfo;
  private final Transport transport;
  private final boolean defaultTransport;
  private final Logger logger;
  private final Clock clock;
  private final Sleeper sleeper;
  private final DoubleSupplier random;
  private final String userAgent;

  private final SmsService sms = new SmsService(this);
  private final EmailService email = new EmailService(this);
  private final WhatsAppService whatsapp = new WhatsAppService(this);
  private final BalanceService balance = new BalanceService(this);

  private Unicom360Client(Builder b) {
    String key =
        b.apiKey != null ? b.apiKey : unsetIfBlank(b.environment.apply("UNICOM360_API_KEY"));
    if (key == null || key.isBlank()) {
      throw new ConfigurationException(
          "No API key: call apiKey(...) on the builder or set UNICOM360_API_KEY.");
    }
    if (key.chars().anyMatch(c -> c < 0x20 || c == 0x7f)) {
      throw new ConfigurationException("The API key must not contain control characters.");
    }
    String url = b.baseUrl;
    if (url == null) {
      url = unsetIfBlank(b.environment.apply("UNICOM360_BASE_URL"));
    }
    this.apiKey = key;
    this.baseUrl = validateBaseUrl(url == null ? DEFAULT_BASE_URL : url);
    this.timeout = positive("timeout", b.timeout);
    this.connectTimeout = positive("connectTimeout", b.connectTimeout);
    if (b.maxRetries < 0) {
      throw new ConfigurationException("maxRetries must be 0 or more");
    }
    this.maxRetries = b.maxRetries;
    if (b.maxRetryWait == null || b.maxRetryWait.isNegative()) {
      throw new ConfigurationException("maxRetryWait must be zero or more");
    }
    this.maxRetryWait = b.maxRetryWait;
    this.autoReferenceIds = b.autoReferenceIds;
    this.appInfo = b.appInfo;
    this.defaultTransport = b.transport == null;
    this.transport = b.transport != null ? b.transport : JdkHttpTransport.create(connectTimeout);
    this.logger = b.logger != null ? b.logger : System.getLogger(LOGGER_NAME);
    this.clock = b.clock != null ? b.clock : Clock.systemUTC();
    this.sleeper = b.sleeper != null ? b.sleeper : Sleeper.SYSTEM;
    this.random = b.random != null ? b.random : () -> ThreadLocalRandom.current().nextDouble();
    this.userAgent = userAgent(appInfo);
  }

  /**
   * A new builder. The defaults are {@code apiKey} from {@code UNICOM360_API_KEY}, {@code baseUrl}
   * from {@code UNICOM360_BASE_URL} else {@value #DEFAULT_BASE_URL}, a 60 s timeout per attempt, a
   * 10 s connect timeout, 2 retries and a 60 s limit on one retry wait.
   *
   * @return the builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A client configured from the environment alone: {@code UNICOM360_API_KEY} and, optionally,
   * {@code UNICOM360_BASE_URL}.
   *
   * @return the client
   * @throws ConfigurationException when the API key is missing or an option is unusable
   */
  public static Unicom360Client fromEnv() {
    return builder().build();
  }

  /**
   * A copy of this client with options changed; the rest are kept.
   *
   * <pre>{@code
   * Unicom360Client patient = client.withOptions(o -> o.timeout(Duration.ofMinutes(2)));
   * }</pre>
   *
   * @param changes changes the options on a builder holding this client's
   * @return the copy
   * @throws ConfigurationException when an option is unusable
   */
  public Unicom360Client withOptions(Consumer<Builder> changes) {
    Builder b = new Builder();
    b.apiKey = apiKey;
    b.baseUrl = baseUrl;
    b.timeout = timeout;
    b.connectTimeout = connectTimeout;
    b.maxRetries = maxRetries;
    b.maxRetryWait = maxRetryWait;
    b.autoReferenceIds = autoReferenceIds;
    b.appInfo = appInfo;
    b.transport = defaultTransport ? null : transport;
    b.logger = logger;
    b.clock = clock;
    b.sleeper = sleeper;
    b.random = random;
    changes.accept(b);
    return b.build();
  }

  /**
   * SMS.
   *
   * @return the SMS operations
   */
  public SmsService sms() {
    return sms;
  }

  /**
   * Email.
   *
   * @return the email operations
   */
  public EmailService email() {
    return email;
  }

  /**
   * WhatsApp: templates, session messages, status and the template list.
   *
   * @return the WhatsApp operations
   */
  public WhatsAppService whatsapp() {
    return whatsapp;
  }

  /**
   * Balances.
   *
   * @return the balance operations
   */
  public BalanceService balance() {
    return balance;
  }

  /**
   * Gateway liveness. Sends no API key.
   *
   * @return the gateway's health
   * @throws Unicom360Exception when the call fails
   */
  public Health health() {
    return health(null);
  }

  /**
   * Gateway liveness. Sends no API key.
   *
   * @param options overrides for this call, or {@code null}
   * @return the gateway's health
   * @throws Unicom360Exception when the call fails
   */
  public Health health(RequestOptions options) {
    return execute(Operation.HEALTH, null, options, Models::health);
  }

  /**
   * The base URL, without a trailing slash.
   *
   * @return the base URL
   */
  public String baseUrl() {
    return baseUrl;
  }

  /**
   * The timeout per attempt.
   *
   * @return the timeout
   */
  public Duration timeout() {
    return timeout;
  }

  /**
   * The connect timeout.
   *
   * @return the connect timeout
   */
  public Duration connectTimeout() {
    return connectTimeout;
  }

  /**
   * The retries after the first attempt.
   *
   * @return the maximum number of retries
   */
  public int maxRetries() {
    return maxRetries;
  }

  /**
   * The upper bound on one computed wait between attempts.
   *
   * @return the bound
   */
  public Duration maxRetryWait() {
    return maxRetryWait;
  }

  /**
   * Whether sends without a {@code user_reference_id} get a generated one.
   *
   * @return whether the option is on
   */
  public boolean autoReferenceIds() {
    return autoReferenceIds;
  }

  /**
   * The User-Agent this client sends.
   *
   * @return the User-Agent
   */
  public String userAgent() {
    return userAgent;
  }

  /**
   * A description with the API key redacted.
   *
   * @return the description
   */
  @Override
  public String toString() {
    return "Unicom360Client[baseUrl="
        + baseUrl
        + ", apiKey=[REDACTED], timeout="
        + timeout
        + ", maxRetries="
        + maxRetries
        + "]";
  }

  // -- the request engine ------------------------------------------------------------------------

  <T> T execute(
      Operation operation,
      JsonObject body,
      RequestOptions options,
      Function<JsonObject, T> parser) {
    int retries =
        options != null && options.maxRetries() != null ? options.maxRetries() : maxRetries;
    Duration attemptTimeout =
        options != null && options.timeout() != null ? options.timeout() : timeout;

    List<String> generated = new ArrayList<>();
    if (autoReferenceIds && operation.send() && body != null) {
      addReferences(operation, body, generated);
    }
    boolean retrySafe = retrySafe(operation, body);

    Map<String, String> headers = new LinkedHashMap<>();
    headers.put("Accept", "application/json");
    headers.put("User-Agent", userAgent);
    if (operation.authenticated()) {
      headers.put("X-API-Key", apiKey);
    }
    byte[] payload = null;
    if ("POST".equals(operation.method())) {
      headers.put("Content-Type", "application/json");
      payload = Json.write(body != null ? body : new JsonObject()).getBytes(StandardCharsets.UTF_8);
    }
    TransportRequest request =
        new TransportRequest(
            operation.method(),
            URI.create(baseUrl + operation.path()),
            headers,
            payload,
            attemptTimeout,
            connectTimeout);

    int attempt = 1;
    while (true) {
      long started = System.nanoTime();
      TransportResponse response = null;
      IntFunction<Unicom360Exception> error;
      boolean retryable;
      boolean rateLimited = false;
      Instant now = null;
      String cause;
      try {
        response = Objects.requireNonNull(transport.send(request), "The transport returned null");
        error = null;
        retryable = false;
        cause = null;
      } catch (TransportException failure) {
        error = connectionError(failure.getMessage(), failure, failure.sent(), failure.timeout());
        retryable = !failure.sent() || retrySafe;
        cause = failure.timeout() ? "timeout_error" : "connection_error";
        logAttempt(operation, cause, null, attempt, started, generated);
      } catch (RuntimeException failure) {
        // Anything the SDK cannot classify may have reached the server.
        error =
            connectionError(
                "The request to the Unicom360 API failed: " + failure, failure, true, false);
        retryable = retrySafe;
        cause = "connection_error";
        logAttempt(operation, cause, null, attempt, started, generated);
      }

      if (response != null) {
        now = clock.instant();
        Interpreter.Outcome outcome = Interpreter.interpret(operation, response, now);
        if (outcome.failure() == null) {
          logAttempt(
              operation,
              Integer.toString(response.status()),
              Json.strictString(outcome.body(), "api_reference"),
              attempt,
              started,
              generated);
          return parser.apply(outcome.body());
        }
        Interpreter.Failure failure = outcome.failure();
        logAttempt(
            operation,
            Integer.toString(response.status()),
            failure.apiReference(),
            attempt,
            started,
            generated);
        rateLimited = failure.rateLimited();
        retryable = rateLimited || ((failure.serverError() || failure.sendFailed()) && retrySafe);
        error = failure::toException;
        cause = failure.describe();
      }

      if (!retryable || attempt > retries) {
        throw error.apply(attempt);
      }
      Duration wait;
      if (rateLimited) {
        Duration base = Interpreter.untilNextMinute(now);
        if (base.compareTo(maxRetryWait) > 0) {
          throw error.apply(attempt);
        }
        wait = base.plus(duration(random.getAsDouble()));
        if (wait.compareTo(Duration.ofSeconds(1)) < 0) {
          wait = Duration.ofSeconds(1);
        }
      } else {
        double limit = Math.min(8.0, Math.min(0.5 * Math.pow(2, attempt), seconds(maxRetryWait)));
        wait = duration(random.getAsDouble() * limit);
      }
      logRetry(operation, attempt, wait, cause);
      try {
        sleeper.sleep(wait);
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
        throw error.apply(attempt);
      }
      attempt++;
    }
  }

  private static IntFunction<Unicom360Exception> connectionError(
      String message, Throwable cause, boolean sent, boolean timedOut) {
    return attempts ->
        timedOut
            ? new ApiTimeoutException(message, cause, sent, attempts)
            : new ApiConnectionException(message, cause, sent, attempts);
  }

  private static void addReferences(Operation operation, JsonObject body, List<String> generated) {
    if (operation == Operation.WHATSAPP_SEND_TEMPLATE) {
      JsonElement recipients = body.get("recipients");
      if (recipients != null && recipients.isJsonArray()) {
        for (JsonElement recipient : recipients.getAsJsonArray()) {
          if (recipient.isJsonObject()) {
            addReference(recipient.getAsJsonObject(), generated);
          }
        }
      }
    } else {
      addReference(body, generated);
    }
  }

  private static void addReference(JsonObject target, List<String> generated) {
    if (!target.has("user_reference_id")) {
      String reference = REFERENCE_PREFIX + UUID.randomUUID().toString().replace("-", "");
      target.addProperty("user_reference_id", reference);
      generated.add(reference);
    }
  }

  private static boolean retrySafe(Operation operation, JsonObject body) {
    switch (operation.retry()) {
      case ALWAYS:
        return true;
      case WITH_REFERENCE:
        return hasReference(body);
      case EVERY_RECIPIENT_REFERENCE:
        JsonArray recipients = body == null ? null : body.getAsJsonArray("recipients");
        if (recipients == null || recipients.isEmpty()) {
          return false;
        }
        for (JsonElement recipient : recipients) {
          if (!recipient.isJsonObject() || !hasReference(recipient.getAsJsonObject())) {
            return false;
          }
        }
        return true;
      default:
        return false;
    }
  }

  private static boolean hasReference(JsonObject object) {
    return Matching.hasText(Json.strictString(object, "user_reference_id"));
  }

  private void logAttempt(
      Operation operation,
      String status,
      String apiReference,
      int attempt,
      long started,
      List<String> generated) {
    if (!logger.isLoggable(Level.DEBUG)) {
      return;
    }
    StringBuilder line =
        new StringBuilder("unicom360 ")
            .append(operation.method())
            .append(' ')
            .append(operation.path())
            .append(" status=")
            .append(status)
            .append(" api_reference=")
            .append(apiReference == null ? "-" : apiReference)
            .append(" attempt=")
            .append(attempt)
            .append(" duration_ms=")
            .append((System.nanoTime() - started) / 1_000_000);
    if (!generated.isEmpty()) {
      line.append(" generated_user_reference_ids=").append(String.join(",", generated));
    }
    logger.log(Level.DEBUG, line.toString());
  }

  private void logRetry(Operation operation, int attempt, Duration wait, String cause) {
    if (!logger.isLoggable(Level.INFO)) {
      return;
    }
    logger.log(
        Level.INFO,
        String.format(
            Locale.ROOT,
            "unicom360 retrying %s %s after attempt=%d wait_s=%.3f cause=%s",
            operation.method(),
            operation.path(),
            attempt,
            seconds(wait),
            cause));
  }

  private static Duration duration(double seconds) {
    return Duration.ofNanos(Math.round(seconds * 1e9));
  }

  private static double seconds(Duration duration) {
    return duration.getSeconds() + duration.getNano() / 1e9;
  }

  // -- configuration ----------------------------------------------------------------------------

  private static String unsetIfBlank(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private static Duration positive(String name, Duration value) {
    if (value == null || value.isZero() || value.isNegative()) {
      throw new ConfigurationException(name + " must be greater than zero");
    }
    return value;
  }

  static String validateBaseUrl(String value) {
    if (value.isEmpty() || value.chars().anyMatch(Character::isWhitespace)) {
      throw new ConfigurationException("baseUrl is not a valid URL: " + value);
    }
    URI uri;
    try {
      uri = new URI(value);
    } catch (URISyntaxException e) {
      throw new ConfigurationException("baseUrl is not a valid URL: " + value);
    }
    String scheme = uri.getScheme();
    String host = uri.getHost();
    boolean https = "https".equalsIgnoreCase(scheme);
    boolean http = "http".equalsIgnoreCase(scheme);
    if ((!https && !http) || host == null || host.isEmpty()) {
      throw new ConfigurationException(
          "baseUrl must be an absolute https URL such as " + DEFAULT_BASE_URL + ": " + value);
    }
    if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
      throw new ConfigurationException("baseUrl must not carry a query string or fragment");
    }
    if (http && !isLoopback(host)) {
      throw new ConfigurationException(
          "baseUrl must use https; plain http is allowed only for localhost, 127.0.0.0/8 and ::1: "
              + value);
    }
    String out = value;
    while (out.endsWith("/")) {
      out = out.substring(0, out.length() - 1);
    }
    return out;
  }

  private static boolean isLoopback(String host) {
    if ("localhost".equalsIgnoreCase(host)) {
      return true;
    }
    if (host.startsWith("[") && host.endsWith("]")) {
      try {
        // A literal: no lookup happens.
        InetAddress address = InetAddress.getByName(host.substring(1, host.length() - 1));
        return address instanceof Inet6Address && address.isLoopbackAddress();
      } catch (UnknownHostException e) {
        return false;
      }
    }
    var matcher = IPV4_LOOPBACK.matcher(host);
    if (!matcher.matches()) {
      return false;
    }
    for (int group = 1; group <= 3; group++) {
      if (Integer.parseInt(matcher.group(group)) > 255) {
        return false;
      }
    }
    return true;
  }

  private static String userAgent(AppInfo app) {
    String agent =
        "unicom360-java/"
            + Unicom360.VERSION
            + " (Java "
            + System.getProperty("java.version", "unknown")
            + "; "
            + osName()
            + "; "
            + System.getProperty("os.arch", "unknown")
            + ")";
    return app == null ? agent : agent + app.userAgentSuffix();
  }

  private static String osName() {
    String name = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);
    if (name.startsWith("mac")) {
      return "darwin";
    }
    if (name.startsWith("windows")) {
      return "windows";
    }
    return name.replace(' ', '-');
  }

  /** Builds a {@link Unicom360Client}. Every option has a default. */
  public static final class Builder {
    private String apiKey;
    private String baseUrl;
    private Duration timeout = Duration.ofSeconds(60);
    private Duration connectTimeout = Duration.ofSeconds(10);
    private int maxRetries = 2;
    private Duration maxRetryWait = Duration.ofSeconds(60);
    private boolean autoReferenceIds;
    private AppInfo appInfo;
    private Transport transport;
    private Logger logger;
    private Clock clock;
    private Sleeper sleeper;
    private DoubleSupplier random;
    private UnaryOperator<String> environment = System::getenv;

    private Builder() {}

    /**
     * Your API key. Without one, {@code UNICOM360_API_KEY} is read; an explicit empty string does
     * not fall back to the environment, it fails.
     *
     * @param apiKey the key, or {@code null} to read the environment
     * @return this builder
     */
    public Builder apiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    /**
     * The base URL. Without one, {@code UNICOM360_BASE_URL} is read, else {@value
     * #DEFAULT_BASE_URL}. It must be {@code https}; plain {@code http} is allowed only for {@code
     * localhost}, {@code 127.0.0.0/8} and {@code ::1}. A path prefix is kept, trailing slashes are
     * removed, and a query string or fragment is refused.
     *
     * @param baseUrl the URL, or {@code null} for the default
     * @return this builder
     */
    public Builder baseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
      return this;
    }

    /**
     * The timeout per attempt, from sending the request to receiving the whole body. Default 60 s.
     *
     * @param timeout more than zero
     * @return this builder
     */
    public Builder timeout(Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    /**
     * The time allowed to connect, TLS included. Default 10 s. It does not apply to a transport you
     * supply.
     *
     * @param connectTimeout more than zero
     * @return this builder
     */
    public Builder connectTimeout(Duration connectTimeout) {
      this.connectTimeout = connectTimeout;
      return this;
    }

    /**
     * The retries after the first attempt, so at most {@code maxRetries + 1} attempts. Default 2.
     *
     * @param maxRetries 0 or more
     * @return this builder
     */
    public Builder maxRetries(int maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    /**
     * The upper bound on one computed wait between attempts. Default 60 s. A 429 whose wait would
     * be longer is thrown at once.
     *
     * @param maxRetryWait zero or more
     * @return this builder
     */
    public Builder maxRetryWait(Duration maxRetryWait) {
      this.maxRetryWait = maxRetryWait;
      return this;
    }

    /**
     * Gives every send without a {@code user_reference_id} a generated one ({@code u360-} and 32
     * hex characters), reused on every retry, which makes SMS and WhatsApp sends retry-safe.
     * Default off. A reference you pass is never replaced.
     *
     * @param autoReferenceIds whether to generate references
     * @return this builder
     */
    public Builder autoReferenceIds(boolean autoReferenceIds) {
      this.autoReferenceIds = autoReferenceIds;
      return this;
    }

    /**
     * Your application, appended to the User-Agent.
     *
     * @param appInfo the application, or {@code null} for none
     * @return this builder
     */
    public Builder appInfo(AppInfo appInfo) {
      this.appInfo = appInfo;
      return this;
    }

    /**
     * Replaces the transport: another HTTP library, a proxy, a fake in your tests. The default is a
     * {@link JdkHttpTransport}.
     *
     * @param transport the transport, or {@code null} for the default
     * @return this builder
     */
    public Builder transport(Transport transport) {
      this.transport = transport;
      return this;
    }

    /**
     * Uses your own {@code HttpClient}, which must not follow redirects.
     *
     * @param httpClient the client
     * @return this builder
     * @throws ConfigurationException when the client follows redirects
     */
    public Builder httpClient(HttpClient httpClient) {
      this.transport = JdkHttpTransport.of(httpClient);
      return this;
    }

    /**
     * The logger. Default {@code System.getLogger("com.unicom360")}. It gets one line per attempt
     * at debug level and one per scheduled retry at info level: never a body, a header, a parameter
     * or the API key.
     *
     * @param logger the logger, or {@code null} for the default
     * @return this builder
     */
    public Builder logger(Logger logger) {
      this.logger = logger;
      return this;
    }

    /**
     * The clock used for 429 waits (test seam). Default the system clock.
     *
     * @param clock the clock, or {@code null} for the default
     * @return this builder
     */
    public Builder clock(Clock clock) {
      this.clock = clock;
      return this;
    }

    /**
     * Waits between attempts (test seam). Default a real sleep.
     *
     * @param sleeper the sleeper, or {@code null} for the default
     * @return this builder
     */
    public Builder sleeper(Sleeper sleeper) {
      this.sleeper = sleeper;
      return this;
    }

    /**
     * The source of retry jitter, a value in {@code [0, 1)} (test seam).
     *
     * @param random the source, or {@code null} for the default
     * @return this builder
     */
    public Builder random(DoubleSupplier random) {
      this.random = random;
      return this;
    }

    Builder environment(UnaryOperator<String> environment) {
      this.environment = environment;
      return this;
    }

    /**
     * Builds the client.
     *
     * @return the client
     * @throws ConfigurationException when the API key is missing or an option is unusable
     */
    public Unicom360Client build() {
      return new Unicom360Client(this);
    }
  }
}
