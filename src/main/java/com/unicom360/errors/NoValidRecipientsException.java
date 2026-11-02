package com.unicom360.errors;

import com.unicom360.internal.Models;
import com.unicom360.models.TemplateResult;
import java.util.List;

/**
 * Every recipient of a template send was rejected ({@code NO_VALID_RECIPIENTS}). {@link #results()}
 * says why for each one. A send where only some recipients are rejected is a result, not this
 * exception.
 */
public final class NoValidRecipientsException extends ApiException {
  private static final long serialVersionUID = 1L;

  private final transient List<TemplateResult> results;

  /**
   * Creates the exception.
   *
   * @param message the gateway's message
   * @param details what the response said; {@code data.results} becomes {@link #results()}
   * @param attempts the HTTP attempts made, retries included
   */
  public NoValidRecipientsException(String message, ErrorDetails details, int attempts) {
    super(message, details, attempts);
    this.results = Models.templateResults(details.data());
  }

  /**
   * One result per recipient, each {@code REJECTED} with its {@code reason}, in request order.
   *
   * @return the results
   */
  public List<TemplateResult> results() {
    return results == null ? List.of() : results;
  }
}
