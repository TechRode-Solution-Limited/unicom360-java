/**
 * The typed results of each operation.
 *
 * <p>Every model is an immutable record with the API's fields (in camelCase), {@code null} when
 * absent or null, plus {@code extra()} (every field this version does not model, so a field the
 * gateway adds tomorrow is readable today) and {@code raw()} (the JSON object it was parsed from).
 * Statuses, categories, reasons and types are plain strings: a value this version does not know
 * passes through. {@code messageId} is always a string.
 */
package com.unicom360.models;
