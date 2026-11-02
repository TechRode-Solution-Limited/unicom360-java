package com.unicom360.params;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.List;

/** A {@code to} field: one string, or a list (sent as given). */
final class Recipients {
  private final String one;
  private final List<String> many;

  private Recipients(String one, List<String> many) {
    this.one = one;
    this.many = many;
  }

  static Recipients one(String to) {
    return to == null ? null : new Recipients(to, null);
  }

  static Recipients many(List<String> to) {
    return to == null ? null : new Recipients(null, List.copyOf(to));
  }

  List<String> asList() {
    return many != null ? many : List.of(one);
  }

  JsonElement toJson() {
    if (many == null) {
      return new JsonPrimitive(one);
    }
    JsonArray array = new JsonArray();
    many.forEach(array::add);
    return array;
  }
}
