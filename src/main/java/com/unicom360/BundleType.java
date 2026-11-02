package com.unicom360;

/**
 * The known bundle types, for {@code client.balance().get(...)}. Types are open: any other string
 * passes through.
 */
public final class BundleType {
  /** SMS to Zambian numbers: the default bundle. */
  public static final String SMS = "SMS";

  /** SMS to numbers outside Zambia. */
  public static final String SMS_INTERNATIONAL = "SMS_INTERNATIONAL";

  /** Email. */
  public static final String EMAIL = "EMAIL";

  /** WhatsApp. */
  public static final String WHATSAPP = "WHATSAPP";

  private BundleType() {}
}
