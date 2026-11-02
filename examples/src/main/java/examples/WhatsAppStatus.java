package examples;

import com.unicom360.Unicom360Client;
import com.unicom360.models.MessageStatus;
import com.unicom360.models.StatusResult;
import java.util.List;

/** Reads the status of WhatsApp messages. Needs UNICOM360_API_KEY. Free. */
public final class WhatsAppStatus {
  private WhatsAppStatus() {}

  public static void main(String[] args) {
    Unicom360Client client = Unicom360Client.fromEnv();
    StatusResult status =
        client
            .whatsapp()
            .status(
                args.length > 0 ? List.of(args) : List.of("7c1f3a2e-5b64-4d2f-9a8e-0f1b2c3d4e5f"));
    for (MessageStatus message : status.results()) {
      System.out.println(message.messageId() + ": " + message.status());
      if (message.error() != null) {
        System.out.println("  " + message.error().code() + " " + message.error().message());
      }
    }
  }
}
