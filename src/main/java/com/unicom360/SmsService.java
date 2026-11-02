package com.unicom360;

import com.unicom360.errors.Unicom360Exception;
import com.unicom360.internal.Models;
import com.unicom360.internal.Operation;
import com.unicom360.models.SmsSendResult;
import com.unicom360.params.SmsSendParams;
import java.util.Objects;

/** The SMS operations: {@code client.sms()}. */
public final class SmsService {
  private final Unicom360Client client;

  SmsService(Unicom360Client client) {
    this.client = client;
  }

  /**
   * Sends one GSM-7 message to 1 to 2,000 numbers. A recipient with {@code Invalid mobile number}
   * is part of the result, not an exception. Retried after a failure that may have reached the
   * gateway only when the params carry a {@code user_reference_id}.
   *
   * @param params the message
   * @return one result per recipient
   * @throws Unicom360Exception when the call fails or the gateway refuses it
   */
  public SmsSendResult send(SmsSendParams params) {
    return send(params, null);
  }

  /**
   * Sends one GSM-7 message to 1 to 2,000 numbers.
   *
   * @param params the message
   * @param options overrides for this call, or {@code null}
   * @return one result per recipient
   * @throws Unicom360Exception when the call fails or the gateway refuses it
   */
  public SmsSendResult send(SmsSendParams params, RequestOptions options) {
    Objects.requireNonNull(params, "params");
    return client.execute(Operation.SMS_SEND, params.toJson(), options, Models::smsSendResult);
  }
}
