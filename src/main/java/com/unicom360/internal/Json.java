package com.unicom360.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** JSON helpers on Gson's tree model. Not part of the public API. */
public final class Json {
  private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
  private static final TypeAdapter<JsonElement> TREE = GSON.getAdapter(JsonElement.class);

  private Json() {}

  /**
   * Parses {@code text} as strict JSON (RFC 8259), keeping numbers exactly as written.
   *
   * @return the value, or {@code null} for an empty text or anything that is not strict JSON
   */
  public static JsonElement parseOrNull(String text) {
    if (text == null || text.isEmpty()) {
      return null;
    }
    try (JsonReader reader = new JsonReader(new StringReader(text))) {
      reader.setStrictness(Strictness.STRICT);
      JsonElement value = TREE.read(reader);
      if (reader.peek() != JsonToken.END_DOCUMENT) {
        return null;
      }
      return value;
    } catch (IOException | RuntimeException e) {
      return null;
    }
  }

  /** Compact JSON text, without HTML escaping. */
  public static String write(JsonElement value) {
    return GSON.toJson(value);
  }

  /** The member's value when it is a JSON object, else {@code null}. */
  public static JsonObject object(JsonObject parent, String key) {
    JsonElement value = parent == null ? null : parent.get(key);
    return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
  }

  /**
   * A string member: a JSON string as is, a number or boolean as its JSON text, anything else
   * (absent, null, object, array) {@code null}.
   */
  public static String string(JsonObject parent, String key) {
    return string(parent == null ? null : parent.get(key));
  }

  /** As {@link #string(JsonObject, String)}, for a value. */
  public static String string(JsonElement value) {
    if (value == null || !value.isJsonPrimitive()) {
      return null;
    }
    return value.getAsJsonPrimitive().getAsString();
  }

  /** A string member only when it is a JSON string, else {@code null}. */
  public static String strictString(JsonObject parent, String key) {
    JsonElement value = parent == null ? null : parent.get(key);
    if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
      return value.getAsString();
    }
    return null;
  }

  /** A boolean member when it is a JSON boolean, else {@code null}. */
  public static Boolean bool(JsonObject parent, String key) {
    JsonElement value = parent == null ? null : parent.get(key);
    if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
      return value.getAsBoolean();
    }
    return null;
  }

  /** An integer member when it is a JSON number with an integral value, else {@code null}. */
  public static Integer integer(JsonObject parent, String key) {
    BigDecimal value = decimal(parent == null ? null : parent.get(key));
    if (value == null) {
      return null;
    }
    try {
      return value.intValueExact();
    } catch (ArithmeticException e) {
      return null;
    }
  }

  /** The exact decimal value of a JSON number, else {@code null}. */
  public static BigDecimal decimal(JsonElement value) {
    if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
      return null;
    }
    try {
      return new BigDecimal(value.getAsJsonPrimitive().getAsString());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * The members of {@code object} whose names are not in {@code known}, in document order, as deep
   * copies.
   */
  public static Map<String, JsonElement> extras(JsonObject object, Set<String> known) {
    Map<String, JsonElement> out = new LinkedHashMap<>();
    if (object != null) {
      for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
        if (!known.contains(entry.getKey())) {
          out.put(entry.getKey(), entry.getValue().deepCopy());
        }
      }
    }
    return out;
  }

  /** An unmodifiable deep copy of an {@code extra} map ({@code null} becomes empty). */
  public static Map<String, JsonElement> copyExtra(Map<String, JsonElement> extra) {
    if (extra == null || extra.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, JsonElement> out = new LinkedHashMap<>();
    for (Map.Entry<String, JsonElement> entry : extra.entrySet()) {
      JsonElement value = entry.getValue();
      out.put(entry.getKey(), value == null ? JsonNull.INSTANCE : value.deepCopy());
    }
    return Collections.unmodifiableMap(out);
  }

  /** A deep copy of {@code raw}, or an empty object for {@code null}. */
  public static JsonObject copy(JsonObject raw) {
    return raw == null ? new JsonObject() : raw.deepCopy();
  }
}
