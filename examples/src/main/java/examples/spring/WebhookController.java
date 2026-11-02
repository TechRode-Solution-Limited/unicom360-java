package examples.spring;

import com.unicom360.Webhooks;
import com.unicom360.errors.SignatureVerificationException;
import com.unicom360.events.MessageReceivedEvent;
import com.unicom360.events.MessageStatusEvent;
import com.unicom360.events.WebhookEvent;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Take the body as {@code byte[]}: the signature covers the exact bytes received, so parsing them
 * to an object first would break verification.
 */
@RestController
public class WebhookController {
  private final String secret = System.getenv("UNICOM360_WEBHOOK_SECRET");

  @PostMapping("/webhooks/unicom360/whatsapp")
  public ResponseEntity<String> whatsapp(
      @RequestBody byte[] rawBody, @RequestHeader HttpHeaders headers) {
    WebhookEvent event;
    try {
      event = Webhooks.constructEvent(rawBody, headers::getFirst, secret);
    } catch (SignatureVerificationException e) {
      return ResponseEntity.status(401).body(e.reason().value());
    }
    if (event instanceof MessageStatusEvent status) {
      System.out.println(status.data().messageId() + " is " + status.data().status());
    } else if (event instanceof MessageReceivedEvent received) {
      System.out.println(received.data().from() + " says: " + received.data().text());
    }
    // Deliveries are retried on any non-2xx: answer quickly and drop repeats by event.deliveryId().
    return ResponseEntity.ok().build();
  }
}
