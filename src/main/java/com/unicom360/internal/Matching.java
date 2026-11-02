package com.unicom360.internal;

import java.util.Locale;

/** The helpers' matching rules (docs/sdk-design.md section 6.3). Not part of the public API. */
public final class Matching {
  private Matching() {}

  /** Every ASCII digit of {@code value}, in order ({@code ""} for {@code null}). */
  public static String digits(String value) {
    if (value == null) {
      return "";
    }
    StringBuilder out = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c >= '0' && c <= '9') {
        out.append(c);
      }
    }
    return out.toString();
  }

  /**
   * Whether {@code candidate} is the number {@code wanted}: the same digits, or, when {@code
   * wanted} has no digits, the same string.
   */
  public static boolean sameNumber(String candidate, String wanted) {
    String digits = digits(wanted);
    if (digits.isEmpty()) {
      return candidate == null ? wanted == null : candidate.equals(wanted);
    }
    return digits.equals(digits(candidate));
  }

  /** Whether two email addresses are equal after trimming whitespace and lower-casing both. */
  public static boolean sameAddress(String a, String b) {
    return normaliseAddress(a).equals(normaliseAddress(b));
  }

  private static String normaliseAddress(String value) {
    return trim(value == null ? "" : value).toLowerCase(Locale.ROOT);
  }

  /** Removes leading and trailing Unicode white space (including no-break spaces). */
  public static String trim(String value) {
    int start = 0;
    int end = value.length();
    while (start < end && isSpace(value.charAt(start))) {
      start++;
    }
    while (end > start && isSpace(value.charAt(end - 1))) {
      end--;
    }
    return value.substring(start, end);
  }

  /** Whether {@code value} has at least one character that is not white space. */
  public static boolean hasText(String value) {
    return value != null && !trim(value).isEmpty();
  }

  private static boolean isSpace(char c) {
    return Character.isWhitespace(c) || Character.isSpaceChar(c) || c == '﻿';
  }
}
