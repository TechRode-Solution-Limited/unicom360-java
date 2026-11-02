package examples;

import com.unicom360.Unicom360Client;
import com.unicom360.models.SmsResult;
import com.unicom360.models.SmsSendResult;
import com.unicom360.params.SmsSendParams;
import java.util.List;

/** Sends one SMS to two numbers. Needs UNICOM360_API_KEY. There is no sandbox: this is billed. */
public final class SendSms {
  private SendSms() {}

  public static void main(String[] args) {
    Unicom360Client client = Unicom360Client.fromEnv();
    SmsSendResult result =
        client
            .sms()
            .send(
                SmsSendParams.builder()
                    .from("Unicom360") // a registered sender ID
                    .to(List.of("260971234567", "260961234567")) // or .to("260971234567")
                    .message("Your order ZM4821 is ready.") // GSM-7, at most 1,200 characters
                    .userReferenceId("order-10421") // optional: makes the send safe to retry
                    .callbackUrl("https://example.com/webhooks/unicom360/sms") // optional
                    .build());

    System.out.println("api_reference: " + result.apiReference());
    for (SmsResult recipient : result.results()) {
      System.out.printf(
          "%s %s %s%n", recipient.mobileNumber(), recipient.status(), recipient.messageId());
    }
    // Results are not in request order past the tenth recipient: find them by number.
    result.byNumber("+260 97 123 4567").ifPresent(r -> System.out.println("found " + r.status()));
  }
}
