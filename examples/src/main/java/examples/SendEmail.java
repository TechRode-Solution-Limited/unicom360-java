package examples;

import com.unicom360.Unicom360Client;
import com.unicom360.models.EmailSendResult;
import com.unicom360.params.EmailSendParams;
import java.util.List;

/** Sends one email to two addresses. Needs UNICOM360_API_KEY. This is billed. */
public final class SendEmail {
  private SendEmail() {}

  public static void main(String[] args) {
    Unicom360Client client = Unicom360Client.fromEnv();
    EmailSendResult result =
        client
            .email()
            .send(
                EmailSendParams.builder()
                    .from("noreply@yourdomain.com")
                    .to(List.of("jane@example.com", "john@example.com"))
                    .subject("Your order is confirmed")
                    .emailBody("<p>Thanks for your order. Order ZM4821 is confirmed.</p>")
                    .replyTo("support@yourdomain.com") // optional
                    .build());

    System.out.println("accepted: " + result.accepted().size());
    result.byAddress("jane@example.com").ifPresent(r -> System.out.println("jane: " + r.status()));
  }
}
