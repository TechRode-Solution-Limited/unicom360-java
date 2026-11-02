package com.unicom360;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Waits between retries. Replace it in tests with {@code Unicom360Client.builder().sleeper(...)}:
 * every retry wait goes through it.
 */
@FunctionalInterface
public interface Sleeper {

  /** The real wait. */
  Sleeper SYSTEM = duration -> TimeUnit.NANOSECONDS.sleep(duration.toNanos());

  /**
   * Waits for {@code duration}.
   *
   * @param duration how long to wait
   * @throws InterruptedException when the thread is interrupted while waiting
   */
  void sleep(Duration duration) throws InterruptedException;
}
