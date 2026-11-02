package examples;

import com.unicom360.Unicom360Client;
import com.unicom360.errors.NoValidRecipientsException;
import com.unicom360.models.TemplateResult;
import com.unicom360.models.TemplateSendResult;
import com.unicom360.params.TemplateBody;
import com.unicom360.params.TemplateButton;
import com.unicom360.params.TemplateHeader;
import com.unicom360.params.TemplateRecipient;
import com.unicom360.params.TemplateSendParams;

/** Sends an approved template to two recipients. Needs UNICOM360_API_KEY. This is billed. */
public final class SendWhatsAppTemplate {
  private SendWhatsAppTemplate() {}

  public static void main(String[] args) {
    Unicom360Client client = Unicom360Client.fromEnv();
    TemplateSendParams params =
        TemplateSendParams.builder()
            .template("order_ready")
            .from("260960123456") // optional: your WhatsApp number
            .language("en_US") // optional
            .addRecipient(
                TemplateRecipient.builder()
                    .to("260971234567")
                    .userReferenceId("inv-42")
                    .header(TemplateHeader.image("https://example.com/receipts/ZM-4821.png"))
                    .body(TemplateBody.of("Ada", "ZM-4821"))
                    .addButton(TemplateButton.url(0, "ZM-4821"))
                    .build())
            .addRecipient(
                TemplateRecipient.builder()
                    .to("260961234567")
                    .userReferenceId("inv-43")
                    .body(TemplateBody.of("Joseph", "ZM-4822"))
                    .build())
            .build();

    try {
      TemplateSendResult result = client.whatsapp().sendTemplate(params);
      System.out.println(
          result.acceptedCount() + " queued, " + result.rejectedCount() + " rejected");
      for (TemplateResult rejected : result.rejected()) {
        System.out.println(rejected.to() + ": " + rejected.reason());
      }
    } catch (NoValidRecipientsException e) {
      e.results().forEach(r -> System.out.println(r.to() + ": " + r.reason()));
    }
  }
}
