package com.unicom360;

import com.unicom360.errors.ConfigurationException;
import java.time.Duration;

/**
 * Overrides for one call. Every operation has an overload taking these.
 *
 * <pre>{@code
 * client.balance().all(RequestOptions.builder().timeout(Duration.ofSeconds(5)).maxRetries(0).build());
 * }</pre>
 */
public final class RequestOptions {
  private final Duration timeout;
  private final Integer maxRetries;

  private RequestOptions(Duration timeout, Integer maxRetries) {
    this.timeout = timeout;
    this.maxRetries = maxRetries;
  }

  /**
   * A new builder.
   *
   * @return the builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * The timeout per attempt, or {@code null} for the client's.
   *
   * @return the timeout, or {@code null}
   */
  public Duration timeout() {
    return timeout;
  }

  /**
   * The retries after the first attempt, or {@code null} for the client's.
   *
   * @return the retries, or {@code null}
   */
  public Integer maxRetries() {
    return maxRetries;
  }

  /** Builds {@link RequestOptions}. */
  public static final class Builder {
    private Duration timeout;
    private Integer maxRetries;

    private Builder() {}

    /**
     * The timeout per attempt, from sending the request to receiving the whole body.
     *
     * @param timeout more than zero, or {@code null} for the client's
     * @return this builder
     * @throws ConfigurationException when it is zero or negative
     */
    public Builder timeout(Duration timeout) {
      if (timeout != null && (timeout.isZero() || timeout.isNegative())) {
        throw new ConfigurationException("timeout must be greater than zero");
      }
      this.timeout = timeout;
      return this;
    }

    /**
     * The retries after the first attempt.
     *
     * @param maxRetries 0 or more
     * @return this builder
     * @throws ConfigurationException when it is negative
     */
    public Builder maxRetries(int maxRetries) {
      if (maxRetries < 0) {
        throw new ConfigurationException("maxRetries must be 0 or more");
      }
      this.maxRetries = maxRetries;
      return this;
    }

    /**
     * Builds the options.
     *
     * @return the options
     */
    public RequestOptions build() {
      return new RequestOptions(timeout, maxRetries);
    }
  }
}
