package com.unicom360.conformance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.unicom360.Unicom360;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** The vendored fixtures are unmodified, and every fixture file has a harness. */
class FixturesMetaTest {
  /** Every fixture file; each has a test class above that runs all of its vectors. */
  private static final Set<String> HARNESSED =
      Set.of(
          "requests.json",
          "responses.json",
          "retries.json",
          "error-codes.json",
          "callbacks.json",
          "gsm7.json",
          "webhooks/signatures.json",
          "webhooks/events.json");

  private static Map<String, String> version() throws IOException {
    Map<String, String> out = new HashMap<>();
    for (String line :
        Files.readAllLines(Harness.FIXTURES.resolve("VERSION"), StandardCharsets.UTF_8)) {
      int equals = line.indexOf('=');
      if (equals > 0) {
        out.put(line.substring(0, equals), line.substring(equals + 1));
      }
    }
    return out;
  }

  private static List<String> relativeFiles() throws IOException {
    List<String> files = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(Harness.FIXTURES)) {
      walk.filter(Files::isRegularFile)
          .forEach(p -> files.add(Harness.FIXTURES.relativize(p).toString().replace('\\', '/')));
    }
    files.sort((a, b) ->
        Arrays.compareUnsigned(
            a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8)));
    return files;
  }

  /** Recomputes the tree hash {@code scripts/sync-conformance} wrote (its {@code --check}). */
  @Test
  void theTreeHashMatchesVersion() throws IOException, NoSuchAlgorithmException {
    StringBuilder listing = new StringBuilder();
    for (String file : relativeFiles()) {
      if (!file.equals("VERSION")) {
        byte[] bytes = Files.readAllBytes(Harness.FIXTURES.resolve(file));
        listing
            .append(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)))
            .append("  ")
            .append(file)
            .append('\n');
      }
    }
    String tree =
        HexFormat.of()
            .formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(listing.toString().getBytes(StandardCharsets.UTF_8)));
    Map<String, String> version = version();
    assertEquals("unicom360-openapi", version.get("source"));
    assertEquals(version.get("sha256"), tree, "the vendored fixtures were modified");
  }

  @Test
  void theFixturesTargetThisContractVersion() throws IOException {
    Map<String, String> version = version();
    assertEquals(Unicom360.CONTRACT_VERSION, version.get("version"));
    assertEquals("v" + Unicom360.CONTRACT_VERSION, version.get("tag"));
    for (String name : HARNESSED) {
      assertEquals(
          Unicom360.CONTRACT_VERSION, Harness.load(name).get("version").getAsString(), name);
    }
  }

  @Test
  void everyFixtureFileHasAHarness() throws IOException {
    Set<String> found = new TreeSet<>();
    for (String file : relativeFiles()) {
      if (file.endsWith(".json") && !file.startsWith("schema/")) {
        found.add(file);
      }
    }
    assertEquals(
        new TreeSet<>(HARNESSED), found, "a fixture file without a harness would be skipped");
  }

  @Test
  void vectorIdsAreUniquePerFile() {
    for (String name : HARNESSED) {
      List<JsonObject> vectors = Harness.vectors(Harness.load(name));
      assertFalse(vectors.isEmpty(), name);
      Set<String> ids = new TreeSet<>();
      vectors.forEach(v -> assertTrue(ids.add(v.get("id").getAsString()), name + ": duplicate id"));
    }
  }
}
