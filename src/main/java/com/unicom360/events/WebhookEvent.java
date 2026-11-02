package com.unicom360.events;

import com.google.gson.JsonObject;

/**
 * A verified webhook delivery, parsed: a {@link MessageStatusEvent}, {@link MessageReceivedEvent},
 * {@link TestEvent} or, for an event this version does not know, {@link UnknownEvent}.
 *
 * <p>Deliveries are retried on any non-2xx answer (after 30 s, 2 min, 10 min, 30 min, 1 h, 3 h, 6 h
 * and 12 h), so answer 2xx quickly and drop repeats by {@link #deliveryId()}.
 */
public sealed interface WebhookEvent
    permits MessageStatusEvent, MessageReceivedEvent, TestEvent, UnknownEvent {

  /**
   * The body's {@code event}, for example {@code message.status}.
   *
   * @return the event name
   */
  String event();

  /**
   * The {@code X-Unicom360-Delivery} header: the same for every retry of one delivery.
   *
   * @return the delivery id, or {@code null} when the header is absent
   */
  String deliveryId();

  /**
   * The body's {@code created_at} (ISO 8601).
   *
   * @return the time, or {@code null}
   */
  String createdAt();

  /**
   * The JSON body, unmodified.
   *
   * @return a copy of the body
   */
  JsonObject raw();
}
