/**
 * The official Java client for the Unicom360 API: SMS, email and WhatsApp over one JSON API.
 *
 * <p>Start with {@link com.unicom360.Unicom360Client}. Webhook helpers are in {@link
 * com.unicom360.Webhooks} and the opt-in GSM-7 helpers in {@link com.unicom360.Gsm}.
 */
module com.unicom360 {
  requires transitive com.google.gson;
  requires transitive java.net.http;

  exports com.unicom360;
  exports com.unicom360.errors;
  exports com.unicom360.events;
  exports com.unicom360.models;
  exports com.unicom360.params;
  exports com.unicom360.transport;
}
