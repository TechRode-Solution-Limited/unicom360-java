package examples;

import com.unicom360.AppInfo;
import com.unicom360.RequestOptions;
import com.unicom360.Unicom360Client;
import com.unicom360.models.Health;
import java.time.Duration;

/** Checks the gateway is up. Sends no API key, though the client still needs one configured. */
public final class CheckHealth {
  private CheckHealth() {}

  public static void main(String[] args) {
    Unicom360Client client =
        Unicom360Client.builder()
            .appInfo(new AppInfo("shopsync", "2.3.0", "https://shopsync.example"))
            .build();
    Health health =
        client.health(
            RequestOptions.builder().timeout(Duration.ofSeconds(5)).maxRetries(0).build());
    System.out.println(health.status() + " " + health.version() + " " + health.environment());
  }
}
