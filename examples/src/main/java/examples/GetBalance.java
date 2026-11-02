package examples;

import com.unicom360.BundleType;
import com.unicom360.Unicom360Client;
import com.unicom360.models.Balance;

/** Reads one bundle's balance. Needs UNICOM360_API_KEY. Free. */
public final class GetBalance {
  private GetBalance() {}

  public static void main(String[] args) {
    Unicom360Client client = Unicom360Client.fromEnv();
    Balance sms = client.balance().get(); // the SMS bundle
    Balance whatsapp = client.balance().get(BundleType.WHATSAPP);
    System.out.println("SMS: " + sms.balance());
    System.out.println("WhatsApp: " + whatsapp.balance());
  }
}
