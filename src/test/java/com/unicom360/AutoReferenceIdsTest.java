package com.unicom360;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.unicom360.errors.ApiTimeoutException;
import com.unicom360.params.EmailSendParams;
import com.unicom360.params.SessionMessage;
import com.unicom360.params.SmsSendParams;
import com.unicom360.params.TemplateRecipient;
import com.unicom360.params.TemplateSendParams;
import com.unicom360.transport.Transport;
import com.unicom360.transport.TransportException;
import com.unicom360.transport.TransportRequest;
import com.unicom360.transport.TransportResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AutoReferenceIdsTest {
  private static final String SMS_OK =
      "{\"api_reference\":\"1\",\"message\":\"successful\",\"success\":true,\"data\":[]}";
  private static final String TEMPLATE_OK =
      "{\"api_reference\":\"1\",\"message\":\"successful\",\"success\":true,"
          + "\"data\":{\"accepted\":0,\"rejected\":0,\"results\":[]}}";
  private static final String SESSION_OK =
      "{\"api_reference\":\"1\",\"message\":\"successful\",\"success\":true,"
          + "\"data\":{\"message_id\":\"m\",\"to\":\"t\",\"status\":\"ACKNOWLEDGED\",\"category\":\"SERVICE\"}}";

  private static SmsSendParams sms() {
    return SmsSendParams.builder().from("Unicom360").to("260971234567").message("Hi").build();
  }

  /** Fails {@code failures} attempts as "timed out after sending", then answers {@code body}. */
  private static Transport transport(List<TransportRequest> requests, int failures, String body) {
    return request -> {
      requests.add(request);
      if (requests.size() <= failures) {
        throw new TransportException("timed out", true, true);
      }
      return TransportResponse.of(200, body);
    };
  }

  private static Unicom360Client client(Transport transport, boolean auto) {
    return Unicom360Client.builder()
        .apiKey("k")
        .baseUrl(Unicom360Client.DEFAULT_BASE_URL)
        .autoReferenceIds(auto)
        .transport(transport)
        .sleeper(d -> {})
        .build();
  }

  private static JsonObject body(TransportRequest request) {
    return JsonParser.parseString(request.bodyText()).getAsJsonObject();
  }

  private static void assertGenerated(String reference) {
    assertTrue(reference.matches("u360-[0-9a-f]{32}"), reference);
  }

  @Test
  void offByDefault() {
    List<TransportRequest> requests = new ArrayList<>();
    client(transport(requests, 0, SMS_OK), false).sms().send(sms());
    assertTrue(!body(requests.get(0)).has("user_reference_id"));
  }

  @Test
  void aGeneratedReferenceIsReusedOnEveryRetryAndMakesTheSendRetrySafe() {
    List<TransportRequest> requests = new ArrayList<>();
    client(transport(requests, 2, SMS_OK), true).sms().send(sms());
    assertEquals(3, requests.size());
    String first = body(requests.get(0)).get("user_reference_id").getAsString();
    assertGenerated(first);
    assertEquals(first, body(requests.get(1)).get("user_reference_id").getAsString());
    assertEquals(first, body(requests.get(2)).get("user_reference_id").getAsString());
  }

  @Test
  void withoutAReferenceASendThatMayHaveLeftIsNotRetried() {
    List<TransportRequest> requests = new ArrayList<>();
    ApiTimeoutException error =
        assertThrows(
            ApiTimeoutException.class,
            () -> client(transport(requests, 2, SMS_OK), false).sms().send(sms()));
    assertEquals(1, requests.size());
    assertEquals(1, error.attempts());
    assertTrue(error.sent());
  }

  @Test
  void aReferenceYouPassIsNeverReplaced() {
    List<TransportRequest> requests = new ArrayList<>();
    client(transport(requests, 0, SMS_OK), true)
        .sms()
        .send(sms().toBuilder().userReferenceId("mine").build());
    assertEquals("mine", body(requests.get(0)).get("user_reference_id").getAsString());
  }

  @Test
  void aBlankReferenceIsKeptButIsNotRetrySafe() {
    List<TransportRequest> requests = new ArrayList<>();
    Unicom360Client client = client(transport(requests, 1, SMS_OK), true);
    assertThrows(
        ApiTimeoutException.class,
        () -> client.sms().send(sms().toBuilder().userReferenceId("  \t").build()));
    assertEquals("  \t", body(requests.get(0)).get("user_reference_id").getAsString());
    assertEquals(1, requests.size());
  }

  @Test
  void anEmailGetsAReferenceButStaysUnsafeToRetry() {
    List<TransportRequest> requests = new ArrayList<>();
    EmailSendParams params =
        EmailSendParams.builder()
            .from("a@example.com")
            .to("b@example.com")
            .subject("Hi")
            .emailBody("<p>Hi</p>")
            .build();
    assertThrows(
        ApiTimeoutException.class,
        () -> client(transport(requests, 1, SMS_OK), true).email().send(params));
    assertEquals(1, requests.size());
    assertGenerated(body(requests.get(0)).get("user_reference_id").getAsString());
  }

  @Test
  void aSessionMessageGetsAReference() {
    List<TransportRequest> requests = new ArrayList<>();
    client(transport(requests, 1, SESSION_OK), true)
        .whatsapp()
        .sendSession(SessionMessage.text("260971234567", "Hi"));
    assertEquals(2, requests.size());
    assertGenerated(body(requests.get(0)).get("user_reference_id").getAsString());
  }

  @Test
  void eachTemplateRecipientWithoutAReferenceGetsItsOwn() {
    List<TransportRequest> requests = new ArrayList<>();
    client(transport(requests, 1, TEMPLATE_OK), true)
        .whatsapp()
        .sendTemplate(
            TemplateSendParams.builder()
                .template("t")
                .addRecipient(TemplateRecipient.of("1"))
                .addRecipient(TemplateRecipient.builder().to("2").userReferenceId("mine").build())
                .addRecipient(TemplateRecipient.of("3"))
                .build());
    assertEquals(2, requests.size(), "every recipient has a reference now, so it is retry-safe");
    var recipients = body(requests.get(0)).getAsJsonArray("recipients");
    String one = recipients.get(0).getAsJsonObject().get("user_reference_id").getAsString();
    String three = recipients.get(2).getAsJsonObject().get("user_reference_id").getAsString();
    assertGenerated(one);
    assertGenerated(three);
    assertNotEquals(one, three);
    assertEquals(
        "mine", recipients.get(1).getAsJsonObject().get("user_reference_id").getAsString());
  }

  @Test
  void readsAndUnreferencedTemplatesAreLeftAlone() {
    List<TransportRequest> requests = new ArrayList<>();
    Transport transport =
        request -> {
          requests.add(request);
          return TransportResponse.of(200, "{\"status\":true,\"balances\":{}}");
        };
    client(transport, true).balance().all();
    assertEquals("{}", requests.get(0).bodyText());
  }
}
