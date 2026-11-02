package examples;

import com.unicom360.Unicom360Client;
import com.unicom360.models.TemplateInfo;
import com.unicom360.models.TemplateList;

/** Lists the approved WhatsApp templates. Needs UNICOM360_API_KEY. Free. */
public final class ListWhatsAppTemplates {
  private ListWhatsAppTemplates() {}

  public static void main(String[] args) {
    Unicom360Client client = Unicom360Client.fromEnv();
    TemplateList templates = client.whatsapp().templates();
    for (TemplateInfo template : templates.templates()) {
      System.out.println(template.name() + " (" + template.language() + ") " + template.category());
    }
    templates
        .byName("order_ready", "en_US")
        .ifPresent(t -> System.out.println("order_ready body: " + t.body().text()));
  }
}
