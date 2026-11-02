package com.unicom360;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.unicom360.params.ContactCard;
import com.unicom360.params.ContentMessage;
import com.unicom360.params.EmailSendParams;
import com.unicom360.params.ListRow;
import com.unicom360.params.ListSection;
import com.unicom360.params.ReactionMessage;
import com.unicom360.params.ReplyButton;
import com.unicom360.params.SessionMessage;
import com.unicom360.params.SmsSendParams;
import com.unicom360.params.TemplateBody;
import com.unicom360.params.TemplateButton;
import com.unicom360.params.TemplateHeader;
import com.unicom360.params.TemplateRecipient;
import com.unicom360.params.TemplateSendParams;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ParamsTest {
  private static JsonObject json(String text) {
    return JsonParser.parseString(text).getAsJsonObject();
  }

  @Test
  void requiredFieldsAreEnforcedByTheBuilders() {
    assertThrows(NullPointerException.class, () -> SmsSendParams.builder().build());
    assertThrows(
        NullPointerException.class, () -> SmsSendParams.builder().from("a").to("b").build());
    assertThrows(NullPointerException.class, () -> EmailSendParams.builder().from("a").build());
    assertThrows(NullPointerException.class, () -> TemplateSendParams.builder().build());
    assertThrows(NullPointerException.class, () -> TemplateRecipient.builder().build());
    assertThrows(NullPointerException.class, () -> ContactCard.builder().build());
    assertThrows(NullPointerException.class, () -> SessionMessage.text(null, "x"));
    assertThrows(NullPointerException.class, () -> SessionMessage.reaction("1", null, ""));
  }

  @Test
  void noBusinessValidationHappens() {
    SmsSendParams params =
        SmsSendParams.builder().from("").to(List.of()).message("😀".repeat(5000)).build();
    assertEquals("", params.from());
    assertEquals(List.of(), params.to());
    assertEquals(
        json("{\"from\":\"\",\"to\":[],\"message\":\"" + "😀".repeat(5000) + "\"}"),
        params.toJson());
    assertEquals(
        "",
        SessionMessage.reaction("1", "m", "")
            .toJson()
            .getAsJsonObject("reaction")
            .get("emoji")
            .getAsString());
  }

  @Test
  void toIsSentAsGiven() {
    assertEquals(
        json("{\"from\":\"f\",\"to\":\"1\",\"message\":\"m\"}"),
        SmsSendParams.builder().from("f").to("1").message("m").build().toJson());
    assertEquals(
        json("{\"from\":\"f\",\"to\":[\"1\"],\"message\":\"m\"}"),
        SmsSendParams.builder().from("f").to(List.of("1")).message("m").build().toJson());
    assertEquals(List.of("1"), SmsSendParams.builder().from("f").to("1").message("m").build().to());
  }

  @Test
  void toBuilderRoundTrips() {
    SmsSendParams sms =
        SmsSendParams.builder()
            .from("f")
            .to(List.of("1"))
            .message("m")
            .userReferenceId("r")
            .callbackUrl("c")
            .source("s")
            .build();
    assertEquals(sms.toJson(), sms.toBuilder().build().toJson());
    EmailSendParams email =
        EmailSendParams.builder()
            .from("f")
            .to("1")
            .subject("s")
            .emailBody("b")
            .replyTo("r")
            .userReferenceId("u")
            .callbackUrl("c")
            .source("x")
            .build();
    assertEquals(email.toJson(), email.toBuilder().build().toJson());
    assertEquals("b", email.emailBody());
    TemplateRecipient recipient =
        TemplateRecipient.builder()
            .to("1")
            .userReferenceId("u")
            .header(TemplateHeader.text("h"))
            .body(TemplateBody.of("a", "b"))
            .buttons(List.of(TemplateButton.url(0, "v")))
            .otp("o")
            .build();
    assertEquals(recipient.toJson(), recipient.toBuilder().build().toJson());
    TemplateSendParams template =
        TemplateSendParams.builder()
            .template("t")
            .from("f")
            .language("l")
            .callbackUrl("c")
            .addRecipient(recipient)
            .build();
    assertEquals(template.toJson(), template.toBuilder().build().toJson());
    assertEquals(List.of(recipient), template.recipients());
  }

  @Test
  void headersAndBodiesHaveTheirWireShapes() {
    assertEquals("Ada", TemplateHeader.text("Ada").toJson().getAsString());
    assertEquals(
        json("{\"type\":\"document\",\"url\":\"u\",\"filename\":\"f\"}"),
        TemplateHeader.document("u", "f").toJson());
    assertEquals(
        json("{\"type\":\"document\",\"url\":\"u\"}"), TemplateHeader.document("u").toJson());
    assertEquals(json("{\"type\":\"image\",\"url\":\"u\"}"), TemplateHeader.image("u").toJson());
    assertEquals(json("{\"type\":\"video\",\"url\":\"u\"}"), TemplateHeader.video("u").toJson());
    assertEquals(
        JsonParser.parseString("[\"a\",\"b\"]"), TemplateBody.of(Arrays.asList("a", "b")).toJson());
    Map<String, String> named = new LinkedHashMap<>();
    named.put("name", "Ada");
    assertEquals(json("{\"name\":\"Ada\"}"), TemplateBody.named(named).toJson());
    assertEquals(
        json("{\"index\":0,\"type\":\"quick_reply\",\"value\":\"v\"}"),
        TemplateButton.quickReply(0, "v").toJson());
  }

  @Test
  void settersReturnCopies() {
    ContentMessage message = SessionMessage.text("1", "hi");
    ContentMessage copy = message.from("f").replyTo("r").userReferenceId("u").callbackUrl("c");
    assertNotSame(message, copy);
    assertNull(message.from());
    assertEquals("f", copy.from());
    assertEquals("r", copy.replyTo());
    assertEquals("u", copy.userReferenceId());
    assertEquals("c", copy.callbackUrl());
    assertEquals("text", copy.type());
    assertEquals("1", copy.to());
    assertFalse(copy.toString().contains("hi"), "toString shows no content");
    assertFalse(copy.toString().contains("1"), "toString shows no number");
  }

  @Test
  void aReactionHasNoReplyTo() {
    ReactionMessage reaction =
        SessionMessage.reaction("1", "m", "x").from("f").userReferenceId("u").callbackUrl("c");
    assertEquals("reaction", reaction.type());
    assertEquals("m", reaction.messageId());
    assertEquals("x", reaction.emoji());
    assertEquals("f", reaction.from());
    assertEquals("u", reaction.userReferenceId());
    assertEquals("c", reaction.callbackUrl());
    assertEquals("1", reaction.to());
    assertTrue(
        Arrays.stream(ReactionMessage.class.getMethods())
            .noneMatch(m -> m.getName().equals("replyTo")),
        "the reaction builder offers no reply_to");
    assertFalse(reaction.toJson().has("reply_to"));
  }

  @Test
  void everyFactoryOverloadBuilds() {
    assertEquals(
        json("{\"to\":\"1\",\"type\":\"image\",\"media\":{\"url\":\"u\"}}"),
        SessionMessage.image("1", "u").toJson());
    assertEquals(
        json("{\"to\":\"1\",\"type\":\"video\",\"media\":{\"url\":\"u\",\"caption\":\"c\"}}"),
        SessionMessage.video("1", "u", "c").toJson());
    assertEquals(
        json("{\"to\":\"1\",\"type\":\"document\",\"media\":{\"url\":\"u\"}}"),
        SessionMessage.document("1", "u").toJson());
    assertEquals(
        json("{\"to\":\"1\",\"type\":\"document\",\"media\":{\"url\":\"u\",\"caption\":\"c\"}}"),
        SessionMessage.document("1", "u", "c").toJson());
    assertEquals(
        json(
            "{\"to\":\"1\",\"type\":\"interactive\",\"interactive\":{\"type\":\"button\",\"body\":\"b\","
                + "\"buttons\":[{\"id\":\"i\",\"title\":\"t\"}],\"header\":\"h\",\"footer\":\"f\"}}"),
        SessionMessage.buttons("1", "b", List.of(ReplyButton.of("i", "t")), "h", "f").toJson());
    assertEquals(
        json(
            "{\"to\":\"1\",\"type\":\"interactive\",\"interactive\":{\"type\":\"list\",\"body\":\"b\",\"button\":\"o\","
                + "\"sections\":[{\"rows\":[{\"id\":\"i\",\"title\":\"t\"}]}],\"header\":\"h\",\"footer\":\"f\"}}"),
        SessionMessage.list(
                "1", "b", "o", List.of(ListSection.of(List.of(ListRow.of("i", "t")))), "h", "f")
            .toJson());
    assertEquals(
        json("{\"to\":\"1\",\"type\":\"location\",\"location\":{\"latitude\":0,\"longitude\":0}}"),
        normalise(SessionMessage.location("1", 0, 0).toJson()));
  }

  /** {@code 0.0} is written by Gson as a double; compare the zero by integer value. */
  private static JsonObject normalise(JsonObject message) {
    JsonObject location = message.getAsJsonObject("location");
    location.addProperty("latitude", location.get("latitude").getAsInt());
    location.addProperty("longitude", location.get("longitude").getAsInt());
    return message;
  }

  @Test
  void aContactCardHasEveryPartInMetasShape() {
    ContactCard card =
        ContactCard.builder()
            .formattedName("F")
            .firstName("a")
            .lastName("b")
            .middleName("c")
            .suffix("d")
            .prefix("e")
            .phone("1")
            .phone("2", "WORK")
            .phone("3", "CELL", "w")
            .email("x@example.com")
            .email("y@example.com", "HOME")
            .url("https://a.example")
            .url("https://b.example", "WORK")
            .address(new ContactCard.Address("s", "c", "st", "z", "co", "cc", "t"))
            .org("Co")
            .org("Co", "D", "T")
            .birthday("1990-01-02")
            .build();
    assertEquals(
        json(
            "{\"name\":{\"formatted_name\":\"F\",\"first_name\":\"a\",\"last_name\":\"b\",\"middle_name\":\"c\","
                + "\"suffix\":\"d\",\"prefix\":\"e\"},"
                + "\"phones\":[{\"phone\":\"1\"},{\"phone\":\"2\",\"type\":\"WORK\"},{\"phone\":\"3\",\"type\":\"CELL\",\"wa_id\":\"w\"}],"
                + "\"emails\":[{\"email\":\"x@example.com\"},{\"email\":\"y@example.com\",\"type\":\"HOME\"}],"
                + "\"urls\":[{\"url\":\"https://a.example\"},{\"url\":\"https://b.example\",\"type\":\"WORK\"}],"
                + "\"addresses\":[{\"street\":\"s\",\"city\":\"c\",\"state\":\"st\",\"zip\":\"z\",\"country\":\"co\","
                + "\"country_code\":\"cc\",\"type\":\"t\"}],"
                + "\"org\":{\"company\":\"Co\",\"department\":\"D\",\"title\":\"T\"},"
                + "\"birthday\":\"1990-01-02\"}"),
        card.toJson());
    assertEquals(
        json("{\"name\":{\"formatted_name\":\"F\"}}"),
        ContactCard.builder().formattedName("F").build().toJson(),
        "unset fields are absent");
  }
}
