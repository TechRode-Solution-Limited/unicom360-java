package com.unicom360;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;

/**
 * Opt-in GSM-7 helpers: the gateway's answers before you send. The SDK never checks these itself.
 *
 * <p>The gateway accepts only GSM-7 text (the basic set plus {@code ^{}\[~]|€}, no form feed), at
 * most {@value #MAX_LENGTH} characters, and bills {@code max(1, ceil(length / 160))} units per
 * recipient, where a CR LF pair counts as one character. Those are billing units, not GSM segments.
 */
public final class Gsm {
  /** The gateway's exact character set. */
  public static final String CHARSET =
      "@£$¥èéùìòÇ\nØø\rÅåΔ_ΦΓΛΩΠΨΣΘΞÆæßÉ !\"#¤%&'()*+,-./0123456789:;<=>?¡"
          + "ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿abcdefghijklmnopqrstuvwxyzäöñüà^{}\\[~]|€";

  /** The longest text the gateway accepts, in characters. */
  public static final int MAX_LENGTH = 1200;

  /** The characters one billing unit covers. */
  public static final int UNIT_LENGTH = 160;

  private static final BitSet SET = new BitSet();

  static {
    CHARSET.codePoints().forEach(SET::set);
  }

  private Gsm() {}

  /**
   * Whether every character of {@code text} is in the set. An empty text is GSM-7.
   *
   * @param text the text
   * @return whether it is GSM-7
   */
  public static boolean isGsm7(String text) {
    Objects.requireNonNull(text, "text");
    return text.codePoints().allMatch(SET::get);
  }

  /**
   * The text's length as the gateway counts it: code points, a CR LF pair counting as one.
   *
   * @param text the text
   * @return the length
   */
  public static int length(String text) {
    Objects.requireNonNull(text, "text");
    int count = 0;
    int previous = -1;
    int[] points = text.codePoints().toArray();
    for (int point : points) {
      if (!(point == '\n' && previous == '\r')) {
        count++;
      }
      previous = point;
    }
    return count;
  }

  /**
   * The billing units per recipient: {@code max(1, ceil(length / 160))}.
   *
   * @param text the text
   * @return the units
   */
  public static int units(String text) {
    return Math.max(1, (length(text) + UNIT_LENGTH - 1) / UNIT_LENGTH);
  }

  /**
   * Every character outside the set, in order.
   *
   * @param text the text
   * @return the characters, with their code point indexes
   */
  public static List<NonGsmCharacter> nonGsmCharacters(String text) {
    Objects.requireNonNull(text, "text");
    List<NonGsmCharacter> out = new ArrayList<>();
    int index = 0;
    for (int point : text.codePoints().toArray()) {
      if (!SET.get(point)) {
        out.add(new NonGsmCharacter(new String(Character.toChars(point)), index));
      }
      index++;
    }
    return List.copyOf(out);
  }

  /**
   * What the gateway would refuse about {@code text}, in this order: {@link GsmIssue#EMPTY}, {@link
   * GsmIssue#TOO_LONG}, {@link GsmIssue#NON_GSM}. An empty list means it would accept the text.
   *
   * @param text the text
   * @return the issues
   */
  public static List<GsmIssue> validateSms(String text) {
    Objects.requireNonNull(text, "text");
    List<GsmIssue> issues = new ArrayList<>();
    if (text.chars().allMatch(c -> c == ' ' || c == '\t' || c == '\n' || c == '\r')) {
      issues.add(GsmIssue.EMPTY);
    }
    if (length(text) > MAX_LENGTH) {
      issues.add(GsmIssue.TOO_LONG);
    }
    if (!isGsm7(text)) {
      issues.add(GsmIssue.NON_GSM);
    }
    return List.copyOf(issues);
  }
}
