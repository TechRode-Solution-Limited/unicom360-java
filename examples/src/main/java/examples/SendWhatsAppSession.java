package examples;

import com.unicom360.Unicom360Client;
import com.unicom360.errors.SessionWindowClosedException;
import com.unicom360.models.SessionSendResult;
import com.unicom360.params.ContactCard;
import com.unicom360.params.ListRow;
import com.unicom360.params.ListSection;
import com.unicom360.params.ReplyButton;
import com.unicom360.params.SessionMessage;
import java.util.List;

/** Sends session messages inside the 24-hour window. Needs UNICOM360_API_KEY. This is billed. */
public final class SendWhatsAppSession {
  private SendWhatsAppSession() {}

  public static void main(String[] args) {
    Unicom360Client client = Unicom360Client.fromEnv();
    String customer = "260971234567";
    String inboundMessageId = args.length > 0 ? args[0] : "wamid.HBgMMjYwOTcxMjM0NTY3";

    List<SessionMessage> messages =
        List.of(
            SessionMessage.text(customer, "Your order is ready.")
                .replyTo(inboundMessageId)
                .userReferenceId("s-1"),
            SessionMessage.image(customer, "https://example.com/receipt.png", "Your receipt"),
            SessionMessage.document(
                customer, "https://example.com/invoice.pdf", "Invoice", "invoice.pdf"),
            SessionMessage.audio(customer, "https://example.com/voice-note.ogg"),
            SessionMessage.buttons(
                customer,
                "Collect today or deliver?",
                List.of(
                    ReplyButton.of("collect", "Collect"), ReplyButton.of("deliver", "Deliver"))),
            SessionMessage.list(
                customer,
                "Choose a delivery slot",
                "Pick a slot",
                List.of(
                    ListSection.of(
                        "Today",
                        List.of(
                            ListRow.of("am", "Morning", "08:00 to 12:00"),
                            ListRow.of("pm", "Afternoon"))))),
            SessionMessage.location(
                customer, -15.4167, 28.2833, "Cairo Road branch", "Cairo Road, Lusaka"),
            SessionMessage.contacts(
                customer,
                List.of(
                    ContactCard.builder()
                        .formattedName("Unicom360 Support")
                        .phone("+260960123456", "WORK")
                        .build())),
            SessionMessage.reaction(customer, inboundMessageId, "👍"));

    try {
      for (SessionMessage message : messages) {
        SessionSendResult result = client.whatsapp().sendSession(message);
        System.out.println(message.type() + ": " + result.messageId());
      }
    } catch (SessionWindowClosedException e) {
      System.out.println("The 24-hour window is closed: send a template instead.");
    }
  }
}
