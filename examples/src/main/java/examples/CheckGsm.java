package examples;

import com.unicom360.Gsm;
import com.unicom360.GsmIssue;

/** The opt-in GSM-7 helpers: no network, no API key. */
public final class CheckGsm {
  private CheckGsm() {}

  public static void main(String[] args) {
    String text = args.length > 0 ? String.join(" ", args) : "It’s ready";
    System.out.println("GSM-7: " + Gsm.isGsm7(text));
    System.out.println("length: " + Gsm.length(text));
    System.out.println("units: " + Gsm.units(text));
    Gsm.nonGsmCharacters(text)
        .forEach(c -> System.out.println("not GSM-7: " + c.character() + " at " + c.index()));
    for (GsmIssue issue : Gsm.validateSms(text)) {
      System.out.println("issue: " + issue);
    }
  }
}
