package examples;

import com.unicom360.Unicom360Client;
import com.unicom360.models.Balances;

/** Reads every bundle's balance. Needs UNICOM360_API_KEY. Free. */
public final class GetAllBalances {
  private GetAllBalances() {}

  public static void main(String[] args) {
    Unicom360Client client = Unicom360Client.fromEnv();
    Balances balances = client.balance().all();
    balances.balances().forEach((bundle, units) -> System.out.println(bundle + ": " + units));
  }
}
