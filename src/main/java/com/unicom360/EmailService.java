package com.unicom360;

import com.unicom360.errors.Unicom360Exception;
import com.unicom360.internal.Models;
import com.unicom360.internal.Operation;
import com.unicom360.models.EmailSendResult;
import com.unicom360.params.EmailSendParams;
import java.util.Objects;

/** The email operations: {@code client.email()}. */
public final class EmailService {
  private final Unicom360Client client;

  EmailService(Unicom360Client client) {
    this.client = client;
  }

  /**
   * Sends one email to 1 to 2,000 addresses. An address with {@code Invalid email address} is part
   * of the result, not an exception. Email is never retry-safe, even with a {@code
   * user_reference_id}: it is retried only after a 429 or a failure to connect.
   *
   * @param params the email
   * @return one result per address
   * @throws Unicom360Exception when the call fails or the gateway refuses it
   */
  public EmailSendResult send(EmailSendParams params) {
    return send(params, null);
  }

  /**
   * Sends one email to 1 to 2,000 addresses.
   *
   * @param params the email
   * @param options overrides for this call, or {@code null}
   * @return one result per address
   * @throws Unicom360Exception when the call fails or the gateway refuses it
   */
  public EmailSendResult send(EmailSendParams params, RequestOptions options) {
    Objects.requireNonNull(params, "params");
    return client.execute(Operation.EMAIL_SEND, params.toJson(), options, Models::emailSendResult);
  }
}
