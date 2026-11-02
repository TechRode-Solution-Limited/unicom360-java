package com.unicom360;

import com.unicom360.errors.ConfigurationException;

/**
 * Your application, appended to the User-Agent so support can tell integrations apart: {@code
 * unicom360-java/0.1.0 (Java 21.0.5; linux; amd64) shopsync/2.3.0 (+https://shopsync.example)}.
 *
 * @param name your application's name (required)
 * @param version its version, or {@code null}
 * @param url its URL, or {@code null}
 */
public record AppInfo(String name, String version, String url) {

  /**
   * Refuses what cannot go in a header: no name, or a control character.
   *
   * @param name your application's name (required)
   * @param version its version, or {@code null}
   * @param url its URL, or {@code null}
   */
  public AppInfo {
    if (name == null || name.isBlank()) {
      throw new ConfigurationException("appInfo needs a name");
    }
    check(name);
    check(version);
    check(url);
  }

  /**
   * An application with a name only.
   *
   * @param name your application's name
   * @return the application
   */
  public static AppInfo of(String name) {
    return new AppInfo(name, null, null);
  }

  /**
   * An application with a name and a version.
   *
   * @param name your application's name
   * @param version its version, or {@code null}
   * @return the application
   */
  public static AppInfo of(String name, String version) {
    return new AppInfo(name, version, null);
  }

  private static void check(String value) {
    if (value != null && value.chars().anyMatch(c -> c < 0x20 || c == 0x7f)) {
      throw new ConfigurationException("appInfo must not contain control characters");
    }
  }

  String userAgentSuffix() {
    StringBuilder out = new StringBuilder(" ").append(name);
    if (version != null && !version.isEmpty()) {
      out.append('/').append(version);
    }
    if (url != null && !url.isEmpty()) {
      out.append(" (+").append(url).append(')');
    }
    return out.toString();
  }
}
