package com.unicom360;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class VersionTest {
  private static String pomVersion() throws IOException {
    Properties properties = new Properties();
    try (InputStream in = VersionTest.class.getResourceAsStream("/unicom360-build.properties")) {
      assertNotNull(in, "the filtered test resource is missing");
      properties.load(in);
    }
    return properties.getProperty("project.version");
  }

  @Test
  void theConstantMatchesThePom() throws IOException {
    assertEquals(pomVersion(), Unicom360.VERSION);
  }

  @Test
  void theConstantIsInTheChangelog() throws IOException {
    String changelog =
        Files.readString(
            Path.of(System.getProperty("basedir", "."), "CHANGELOG.md"), StandardCharsets.UTF_8);
    assertTrue(
        changelog.contains("## [" + Unicom360.VERSION + "]"),
        "CHANGELOG.md needs a section for " + Unicom360.VERSION);
  }

  @Test
  void theReadmeAndTheExamplesNameTheVersion() throws IOException {
    Path base = Path.of(System.getProperty("basedir", "."));
    String readme = Files.readString(base.resolve("README.md"), StandardCharsets.UTF_8);
    assertTrue(
        readme.contains("com.unicom360:unicom360-java:" + Unicom360.VERSION),
        "the README's Gradle coordinates");
    assertTrue(
        readme.contains("<version>" + Unicom360.VERSION + "</version>"),
        "the README's Maven coordinates");
    assertTrue(
        readme.contains("Unicom360 API contract v" + Unicom360.CONTRACT_VERSION),
        "the README's targets line");
    String examples = Files.readString(base.resolve("examples/pom.xml"), StandardCharsets.UTF_8);
    assertTrue(
        examples.contains(
            "<artifactId>unicom360-java</artifactId>\n      <version>"
                + Unicom360.VERSION
                + "</version>"),
        "the examples depend on this version");
  }

  @Test
  void theUserAgentCarriesTheVersion() {
    String agent = Unicom360Client.builder().apiKey("k").build().userAgent();
    assertTrue(agent.startsWith("unicom360-java/" + Unicom360.VERSION + " ("), agent);
  }
}
