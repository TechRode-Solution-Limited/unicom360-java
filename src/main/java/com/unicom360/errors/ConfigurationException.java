package com.unicom360.errors;

/** The SDK cannot be used as configured: no API key, an unusable option, a blank webhook secret. */
public final class ConfigurationException extends Unicom360Exception {
  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param message what is wrong with the configuration
   */
  public ConfigurationException(String message) {
    super(message);
  }

  /**
   * Returns {@code configuration_error}.
   *
   * @return the type string
   */
  @Override
  public String type() {
    return "configuration_error";
  }
}
