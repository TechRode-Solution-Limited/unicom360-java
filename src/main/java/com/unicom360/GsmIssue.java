package com.unicom360;

/** Why {@link Gsm#validateSms(String)} says the gateway would refuse a text. */
public enum GsmIssue {
  /** The text is empty, or only spaces, tabs, CR and LF. */
  EMPTY("empty"),
  /** The text is longer than {@link Gsm#MAX_LENGTH}. */
  TOO_LONG("too_long"),
  /** The text has characters outside the GSM-7 set. */
  NON_GSM("non_gsm");

  private final String value;

  GsmIssue(String value) {
    this.value = value;
  }

  /**
   * The issue as every Unicom360 SDK spells it: {@code empty}, {@code too_long} or {@code non_gsm}.
   *
   * @return the issue string
   */
  public String value() {
    return value;
  }

  /**
   * Returns {@link #value()}.
   *
   * @return the issue string
   */
  @Override
  public String toString() {
    return value;
  }
}
