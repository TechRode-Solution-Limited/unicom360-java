package examples;

import com.sun.net.httpserver.HttpServer;
import com.unicom360.Webhooks;
import com.unicom360.errors.SignatureVerificationException;
import com.unicom360.events.MessageReceivedEvent;
import com.unicom360.events.MessageStatusEvent;
import com.unicom360.events.SmsDeliveryReport;
import com.unicom360.events.TestEvent;
import com.unicom360.events.UnknownEvent;
import com.unicom360.events.WebhookEvent;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * A webhook receiver on the JDK's own HTTP server. Set UNICOM360_WEBHOOK_SECRET to the 43-character
 * secret from the portal, and use it as it is: never base64-decode it.
 */
public final class WebhookHttpServer {
  private WebhookHttpServer() {}

  public static void main(String[] args) throws IOException {
    String secret = System.getenv("UNICOM360_WEBHOOK_SECRET");
    HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

    server.createContext(
        "/webhooks/unicom360/whatsapp",
        exchange -> {
          byte[] rawBody = exchange.getRequestBody().readAllBytes();
          int status;
          try {
            WebhookEvent event =
                Webhooks.constructEvent(rawBody, exchange.getRequestHeaders(), secret);
            handle(event);
            status = 200;
          } catch (SignatureVerificationException e) {
            System.out.println("rejected: " + e.reason());
            status = 401;
          }
          exchange.sendResponseHeaders(status, -1);
          exchange.close();
        });

    // SMS and email reports are unsigned and never retried: anyone can post one, so treat them as
    // hints and confirm anything that matters another way.
    server.createContext(
        "/webhooks/unicom360/sms",
        exchange -> {
          SmsDeliveryReport report =
              Webhooks.parseSmsCallback(exchange.getRequestBody().readAllBytes());
          System.out.println(report.messageId() + ": " + report.status());
          exchange.sendResponseHeaders(200, -1);
          exchange.close();
        });

    server.start();
    System.out.println("listening on http://localhost:8080");
  }

  private static void handle(WebhookEvent event) {
    // Deliveries are retried on any non-2xx: answer quickly and drop repeats by event.deliveryId().
    if (event instanceof MessageStatusEvent status) {
      System.out.println(status.data().messageId() + " is " + status.data().status());
    } else if (event instanceof MessageReceivedEvent received) {
      System.out.println(received.data().from() + " says: " + received.data().text());
    } else if (event instanceof TestEvent test) {
      System.out.println("test: " + test.data().message());
    } else if (event instanceof UnknownEvent unknown) {
      System.out.println("an event this version does not know: " + unknown.event());
    }
  }
}
