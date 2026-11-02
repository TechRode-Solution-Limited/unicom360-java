package com.unicom360;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.unicom360.errors.ApiConnectionException;
import com.unicom360.errors.ConfigurationException;
import com.unicom360.models.Health;
import com.unicom360.transport.JdkHttpTransport;
import com.unicom360.transport.Transport;
import com.unicom360.transport.TransportRequest;
import com.unicom360.transport.TransportResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ClientConfigurationTest {
  private static final String HEALTH_BODY =
      "{\"status\":\"ok\",\"timestamp\":\"2026-10-22T08:15:30.412Z\",\"version\":\"0.1.0\","
          + "\"environment\":\"production\"}";

  private static UnaryOperator<String> env(String key, String baseUrl) {
    return name ->
        switch (name) {
          case "UNICOM360_API_KEY" -> key;
          case "UNICOM360_BASE_URL" -> baseUrl;
          default -> null;
        };
  }

  private static Unicom360Client.Builder builder(UnaryOperator<String> environment) {
    return Unicom360Client.builder().environment(environment);
  }

  // -- api_key ------------------------------------------------------------------------------

  @Test
  void aMissingKeyFailsAtConstruction() {
    ConfigurationException error =
        assertThrows(ConfigurationException.class, () -> builder(env(null, null)).build());
    assertTrue(error.getMessage().contains("UNICOM360_API_KEY"));
    assertEquals("configuration_error", error.type());
    assertEquals(0, error.attempts());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "\t\n"})
  void anExplicitBlankKeyFailsWithoutFallingBackToTheEnvironment(String key) {
    assertThrows(
        ConfigurationException.class, () -> builder(env("from-env", null)).apiKey(key).build());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void aBlankKeyInTheEnvironmentCountsAsUnset(String key) {
    assertThrows(ConfigurationException.class, () -> builder(env(key, null)).build());
  }

  @Test
  void theKeyComesFromTheEnvironmentWhenNotGiven() {
    List<TransportRequest> requests = new ArrayList<>();
    Unicom360Client client = builder(env("from-env", null)).transport(answering(requests)).build();
    client.balance().get();
    assertEquals("from-env", requests.get(0).header("X-API-Key"));
  }

  @Test
  void anExplicitKeyWinsOverTheEnvironment() {
    List<TransportRequest> requests = new ArrayList<>();
    Unicom360Client client =
        builder(env("from-env", null)).apiKey("explicit").transport(answering(requests)).build();
    client.balance().get();
    assertEquals("explicit", requests.get(0).header("X-API-Key"));
  }

  @Test
  void aKeyWithControlCharactersIsRefused() {
    assertThrows(
        ConfigurationException.class, () -> builder(env(null, null)).apiKey("a\r\nb").build());
  }

  // -- base_url -----------------------------------------------------------------------------

  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://example.com",
        "http://127.0.0.1.example.com",
        "http://128.0.0.1",
        "http://127.0.0.256",
        "http://[::2]",
        "ftp://api.unicom360.com",
        "api.unicom360.com",
        "//api.unicom360.com",
        "https://",
        "https:///path",
        "https://api.unicom360.com?x=1",
        "https://api.unicom360.com?",
        "https://api.unicom360.com#frag",
        "https://api.unicom360.com#",
        "https://api.unicom360.com/a b",
        " https://api.unicom360.com",
        ""
      })
  void unusableBaseUrlsAreRefused(String url) {
    assertThrows(
        ConfigurationException.class, () -> builder(env("k", null)).baseUrl(url).build(), url);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "https://api.unicom360.com",
        "https://proxy.example.com/unicom360",
        "http://localhost",
        "http://LOCALHOST:4010",
        "http://127.0.0.1:4010",
        "http://127.255.255.254",
        "http://[::1]:4010",
        "http://[0:0:0:0:0:0:0:1]",
        "HTTPS://api.unicom360.com"
      })
  void usableBaseUrlsAreKept(String url) {
    assertEquals(url, builder(env("k", null)).baseUrl(url).build().baseUrl());
  }

  @Test
  void trailingSlashesAreRemovedAndAPrefixIsKept() {
    List<TransportRequest> requests = new ArrayList<>();
    Unicom360Client client =
        builder(env("k", null))
            .baseUrl("https://proxy.example.com/unicom360///")
            .transport(answering(requests))
            .build();
    assertEquals("https://proxy.example.com/unicom360", client.baseUrl());
    client.balance().all();
    assertEquals(
        URI.create("https://proxy.example.com/unicom360/api/v1/json/balance/all"),
        requests.get(0).uri());
  }

  @Test
  void theBaseUrlPrecedenceIsArgumentThenEnvironmentThenDefault() {
    assertEquals(
        "https://arg.example.com",
        builder(env("k", "https://env.example.com"))
            .baseUrl("https://arg.example.com")
            .build()
            .baseUrl());
    assertEquals(
        "https://env.example.com", builder(env("k", "https://env.example.com")).build().baseUrl());
    assertEquals(Unicom360Client.DEFAULT_BASE_URL, builder(env("k", null)).build().baseUrl());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void aBlankBaseUrlInTheEnvironmentFallsBackToTheDefault(String url) {
    assertEquals(Unicom360Client.DEFAULT_BASE_URL, builder(env("k", url)).build().baseUrl());
  }

  @Test
  void anExplicitEmptyBaseUrlFailsWithoutFallingBack() {
    assertThrows(
        ConfigurationException.class,
        () -> builder(env("k", "https://env.example.com")).baseUrl("").build());
  }

  // -- options ------------------------------------------------------------------------------

  @Test
  void theDefaultsAreTheContractsDefaults() {
    Unicom360Client client = builder(env("k", null)).build();
    assertEquals(Duration.ofSeconds(60), client.timeout());
    assertEquals(Duration.ofSeconds(10), client.connectTimeout());
    assertEquals(2, client.maxRetries());
    assertEquals(Duration.ofSeconds(60), client.maxRetryWait());
    assertFalse(client.autoReferenceIds());
  }

  @Test
  void unusableOptionsAreRefused() {
    assertThrows(
        ConfigurationException.class, () -> builder(env("k", null)).timeout(Duration.ZERO).build());
    assertThrows(
        ConfigurationException.class,
        () -> builder(env("k", null)).connectTimeout(Duration.ofSeconds(-1)).build());
    assertThrows(
        ConfigurationException.class, () -> builder(env("k", null)).maxRetries(-1).build());
    assertThrows(
        ConfigurationException.class,
        () -> builder(env("k", null)).maxRetryWait(Duration.ofSeconds(-1)).build());
    assertThrows(
        ConfigurationException.class, () -> RequestOptions.builder().timeout(Duration.ZERO));
    assertThrows(ConfigurationException.class, () -> RequestOptions.builder().maxRetries(-1));
    assertThrows(ConfigurationException.class, () -> new AppInfo(" ", null, null));
    assertThrows(ConfigurationException.class, () -> new AppInfo("a\nb", null, null));
  }

  @Test
  void anHttpClientThatFollowsRedirectsIsRefused() {
    HttpClient following =
        HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    assertThrows(ConfigurationException.class, () -> builder(env("k", null)).httpClient(following));
    HttpClient never = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    assertSame(never, JdkHttpTransport.of(never).httpClient());
  }

  @Test
  void perRequestOptionsOverrideTheClient() {
    List<TransportRequest> requests = new ArrayList<>();
    Unicom360Client client =
        builder(env("k", null))
            .timeout(Duration.ofSeconds(9))
            .transport(answering(requests))
            .build();
    client.balance().all();
    client.balance().all(RequestOptions.builder().timeout(Duration.ofSeconds(3)).build());
    assertEquals(Duration.ofSeconds(9), requests.get(0).timeout());
    assertEquals(Duration.ofSeconds(3), requests.get(1).timeout());

    List<Duration> sleeps = new ArrayList<>();
    Transport refusing =
        request -> {
          requests.add(request);
          throw new RuntimeException("no route");
        };
    Unicom360Client retrying =
        builder(env("k", null)).transport(refusing).maxRetries(5).sleeper(sleeps::add).build();
    requests.clear();
    ApiConnectionException error =
        assertThrows(
            ApiConnectionException.class,
            () -> retrying.balance().all(RequestOptions.builder().maxRetries(1).build()));
    assertEquals(2, error.attempts());
    assertEquals(2, requests.size());
    assertEquals(1, sleeps.size());
  }

  // -- withOptions, toString, User-Agent ------------------------------------------------------

  @Test
  void withOptionsCopiesAndChanges() {
    List<TransportRequest> requests = new ArrayList<>();
    Unicom360Client client =
        builder(env("the-key", null)).maxRetries(1).transport(answering(requests)).build();
    Unicom360Client copy = client.withOptions(o -> o.timeout(Duration.ofSeconds(5)).maxRetries(0));
    assertEquals(Duration.ofSeconds(5), copy.timeout());
    assertEquals(0, copy.maxRetries());
    assertEquals(Duration.ofSeconds(60), client.timeout(), "the original is unchanged");
    assertEquals(1, client.maxRetries());
    copy.balance().all();
    assertEquals("the-key", requests.get(0).header("X-API-Key"), "the key and transport are kept");
  }

  @Test
  void withOptionsValidates() {
    Unicom360Client client = builder(env("k", null)).build();
    assertThrows(
        ConfigurationException.class,
        () -> client.withOptions(o -> o.baseUrl("http://example.com")));
  }

  @Test
  void theKeyNeverAppearsInToString() {
    Unicom360Client client = builder(env(null, null)).apiKey("s3cret-key").build();
    assertTrue(client.toString().contains("apiKey=[REDACTED]"), client.toString());
    assertFalse(client.toString().contains("s3cret-key"));

    TransportRequest request =
        new TransportRequest(
            "POST",
            URI.create("https://api.unicom360.com/x"),
            Map.of("X-API-Key", "s3cret-key"),
            new byte[] {1},
            Duration.ofSeconds(1),
            Duration.ofSeconds(1));
    assertFalse(request.toString().contains("s3cret-key"), request.toString());
    assertTrue(request.toString().contains("[REDACTED]"));
  }

  @Test
  void theUserAgentNamesTheSdkAndRuntime() {
    String agent = builder(env("k", null)).build().userAgent();
    assertTrue(
        agent.matches(
            "unicom360-java/"
                + Unicom360.VERSION.replace(".", "\\.")
                + " \\(Java [^;]+; [^;]+; [^)]+\\)"),
        agent);
  }

  @Test
  void theUserAgentCarriesTheApp() {
    String agent =
        builder(env("k", null))
            .appInfo(new AppInfo("shopsync", "2.3.0", "https://shopsync.example"))
            .build()
            .userAgent();
    assertTrue(agent.endsWith(") shopsync/2.3.0 (+https://shopsync.example)"), agent);
    assertTrue(
        builder(env("k", null))
            .appInfo(AppInfo.of("shopsync"))
            .build()
            .userAgent()
            .endsWith(") shopsync"));
    assertTrue(
        builder(env("k", null))
            .appInfo(AppInfo.of("shopsync", "1"))
            .build()
            .userAgent()
            .endsWith(") shopsync/1"));
  }

  @Test
  void healthSendsNoKey() {
    List<TransportRequest> requests = new ArrayList<>();
    Unicom360Client client =
        builder(env("k", null))
            .transport(request -> {
              requests.add(request);
              return TransportResponse.of(200, HEALTH_BODY);
            })
            .build();
    Health health = client.health();
    assertEquals("ok", health.status());
    assertEquals(null, requests.get(0).header("X-API-Key"));
  }

  private static Transport answering(List<TransportRequest> requests) {
    return request -> {
      requests.add(request);
      return TransportResponse.of(
          200, "{\"status\":true,\"message\":\"m\",\"balance\":1,\"balances\":{\"SMS\":1}}");
    };
  }
}
