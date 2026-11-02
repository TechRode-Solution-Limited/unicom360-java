/**
 * The exceptions the SDK throws, all unchecked and rooted at {@link
 * com.unicom360.errors.Unicom360Exception}.
 *
 * <pre>
 * Unicom360Exception
 * +-- ConfigurationException          no API key, an unusable option
 * +-- ApiConnectionException          no complete response
 * |   +-- ApiTimeoutException
 * +-- ApiStatusException              any other non-2xx, or a 2xx that is not an envelope
 * |   +-- AuthenticationException     401
 * |   +-- RateLimitException          429
 * |   +-- InvalidRequestException     400
 * |   +-- ApiServerException          5xx
 * +-- ApiException                    HTTP 200 with success: false (a business refusal)
 * |   +-- InsufficientBundleException
 * |   +-- DuplicateReferenceException
 * |   +-- NoValidRecipientsException
 * |   +-- SessionWindowClosedException
 * +-- SignatureVerificationException  a webhook failed verification
 * </pre>
 */
package com.unicom360.errors;
