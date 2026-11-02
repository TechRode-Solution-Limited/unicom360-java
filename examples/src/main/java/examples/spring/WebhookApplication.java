package examples.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Runs the Spring Boot webhook receiver. Set UNICOM360_WEBHOOK_SECRET first. */
@SpringBootApplication
public class WebhookApplication {
  public static void main(String[] args) {
    SpringApplication.run(WebhookApplication.class, args);
  }
}
