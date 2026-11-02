package com.unicom360;

/**
 * A character outside the GSM-7 set.
 *
 * @param character the character, as a string (an emoji is one character)
 * @param index its 0-based code point index in the text (a CR LF pair counts as two here)
 */
public record NonGsmCharacter(String character, int index) {}
