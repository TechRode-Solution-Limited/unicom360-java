package com.unicom360.params;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A template recipient's body parameters: positional values for {@code {{1}}}, {@code {{2}}}, ...
 * or named values.
 */
public sealed interface TemplateBody permits TemplateBody.Positional, TemplateBody.Named {

  /**
   * Positional values, in order.
   *
   * @param values the values for {@code {{1}}}, {@code {{2}}}, ...
   * @return the body
   */
  static Positional of(String... values) {
    return new Positional(Arrays.asList(values));
  }

  /**
   * Positional values, in order.
   *
   * @param values the values for {@code {{1}}}, {@code {{2}}}, ...
   * @return the body
   */
  static Positional of(List<String> values) {
    return new Positional(values);
  }

  /**
   * Named values.
   *
   * @param values each variable's name and value, sent in the map's order
   * @return the body
   */
  static Named named(Map<String, String> values) {
    return new Named(values);
  }

  /**
   * The JSON this sends: a list or an object.
   *
   * @return the JSON value
   */
  JsonElement toJson();

  /**
   * Positional values.
   *
   * @param values the values, in order
   */
  record Positional(List<String> values) implements TemplateBody {
    /**
     * Copies the values.
     *
     * @param values the values, in order
     */
    public Positional {
      values = List.copyOf(values);
    }

    @Override
    public JsonElement toJson() {
      JsonArray array = new JsonArray();
      values.forEach(array::add);
      return array;
    }
  }

  /**
   * Named values.
   *
   * @param values each variable's name and value
   */
  record Named(Map<String, String> values) implements TemplateBody {
    /**
     * Copies the values, keeping their order.
     *
     * @param values each variable's name and value
     */
    public Named {
      Map<String, String> copy = new LinkedHashMap<>();
      values.forEach(
          (k, v) -> copy.put(Objects.requireNonNull(k, "name"), Objects.requireNonNull(v, k)));
      values = Collections.unmodifiableMap(copy);
    }

    @Override
    public JsonElement toJson() {
      JsonObject o = new JsonObject();
      values.forEach(o::addProperty);
      return o;
    }
  }
}
