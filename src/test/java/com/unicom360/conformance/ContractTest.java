package com.unicom360.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.unicom360.Unicom360Client;
import com.unicom360.conformance.Harness.SilentLogger;
import com.unicom360.models.Balance;
import com.unicom360.models.Balances;
import com.unicom360.models.EmailSendResult;
import com.unicom360.models.Health;
import com.unicom360.models.SessionSendResult;
import com.unicom360.models.SmsSendResult;
import com.unicom360.models.StatusResult;
import com.unicom360.models.TemplateList;
import com.unicom360.models.TemplateSendResult;
import com.unicom360.params.EmailSendParams;
import com.unicom360.params.SessionMessage;
import com.unicom360.params.SmsSendParams;
import com.unicom360.transport.JdkHttpTransport;
import com.unicom360.transport.Transport;
import com.unicom360.transport.TransportException;
import com.unicom360.transport.TransportRequest;
import com.unicom360.transport.TransportResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * The contract test: every call of {@code requests.json} against a Prism mock of {@code
 * openapi.yaml}, through the default transport. Not part of the default run:
 *
 * <pre>
 * docker run --rm -p 4015:4010 -v "$OPENAPI_DIR":/spec:ro stoplight/prism:5 \
 *     mock --multiprocess=false --errors -h 0.0.0.0 /spec/openapi.yaml
 * UNICOM360_CONTRACT_BASE_URL=http://127.0.0.1:4015 ./mvnw test -Dgroups=contract
 * </pre>
 *
 * <p>Prism validates each request against the spec and answers a request it rejects with the spec's
 * documented 400, and reports every problem in an {@code sl-violations} header: the test asserts a
 * 2xx and no such header. The example Prism answers with must parse into the operation's result.
 */
@Tag("contract")
class ContractTest {
  private static final String BASE_URL =
      System.getenv().getOrDefault("UNICOM360_CONTRACT_BASE_URL", "http://127.0.0.1:4015");

  private static final Map<String, Class<?>> RESULT_TYPES =
      Map.of(
          "sms.send", SmsSendResult.class,
          "email.send", EmailSendResult.class,
          "whatsapp.send_template", TemplateSendResult.class,
          "whatsapp.send_session", SessionSendResult.class,
          "whatsapp.status", StatusResult.class,
          "whatsapp.templates", TemplateList.class,
          "balance.get", Balance.class,
          "balance.all", Balances.class,
          "health", Health.class);

  /** The default transport, recording what went over the wire. */
  private static final class Recording implements Transport {
    private final Transport real = JdkHttpTransport.create(Duration.ofSeconds(5));
    final List<TransportRequest> requests = new ArrayList<>();
    final List<TransportResponse> responses = new ArrayList<>();

    @Override
    public TransportResponse send(TransportRequest request) throws TransportException {
      TransportResponse response = real.send(request);
      requests.add(request);
      responses.add(response);
      return response;
    }

    void assertConforms() {
      assertFalse(responses.isEmpty(), "no request was sent");
      for (int i = 0; i < responses.size(); i++) {
        TransportRequest request = requests.get(i);
        TransportResponse response = responses.get(i);
        String label = request.method() + " " + request.uri();
        assertNull(
            response.header("sl-violations"), label + ": " + response.header("sl-violations"));
        assertTrue(
            response.status() >= 200 && response.status() <= 299,
            label + ": HTTP " + response.status() + " " + response.body());
      }
    }
  }

  private static Unicom360Client client(
      Recording transport, Consumer<Unicom360Client.Builder> options) {
    Unicom360Client.Builder builder =
        Unicom360Client.builder()
            .apiKey("contract-test-key")
            .baseUrl(BASE_URL)
            .maxRetries(0)
            .transport(transport)
            .logger(new SilentLogger());
    options.accept(builder);
    return builder.build();
  }

  @Test
  void prismIsReachable() {
    Recording transport = new Recording();
    assertEquals("ok", client(transport, o -> {}).health().status());
    transport.assertConforms();
  }

  @Test
  void everyOperationIsCovered() {
    Set<String> covered = new java.util.TreeSet<>();
    Harness.vectors(Harness.load("requests.json"))
        .forEach(v -> covered.add(v.get("operation").getAsString()));
    assertEquals(new java.util.TreeSet<>(Harness.OPERATIONS), covered);
  }

  @TestFactory
  Stream<DynamicTest> everyRequestConformsToTheSpec() {
    return Harness.vectors(Harness.load("requests.json")).stream()
        .map(v -> DynamicTest.dynamicTest(v.get("id").getAsString(), () -> check(v)));
  }

  private static void check(JsonObject vector) {
    Recording transport = new Recording();
    String operation = vector.get("operation").getAsString();
    Object result =
        Harness.call(client(transport, o -> {}), operation, vector.getAsJsonObject("params"));
    transport.assertConforms();
    assertNotNull(result);
    assertEquals(RESULT_TYPES.get(operation), result.getClass());
  }

  @Test
  void generatedReferencesConform() {
    Recording transport = new Recording();
    Unicom360Client client = client(transport, o -> o.autoReferenceIds(true));
    client
        .sms()
        .send(SmsSendParams.builder().from("Unicom360").to("260971234567").message("Hi").build());
    client
        .email()
        .send(
            EmailSendParams.builder()
                .from("noreply@example.com")
                .to("jane@example.com")
                .subject("Hi")
                .emailBody("<p>Hi</p>")
                .build());
    client.whatsapp().sendSession(SessionMessage.text("260971234567", "Hi"));
    transport.assertConforms();
    assertEquals(3, transport.requests.size());
  }
}
