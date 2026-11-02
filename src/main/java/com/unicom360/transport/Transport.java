package com.unicom360.transport;

/**
 * Sends one HTTP request and returns the whole response. Implementations must be thread-safe and
 * must never follow redirects (a 3xx is returned as a response, so the API key never reaches
 * another host).
 *
 * <pre>{@code
 * Unicom360Client client = Unicom360Client.builder()
 *     .transport(request -> myHttpLibrary.send(request))
 *     .build();
 * }</pre>
 */
@FunctionalInterface
public interface Transport {

  /**
   * Sends {@code request}.
   *
   * @param request the request; its headers carry the API key, so never log them
   * @return the response, whatever its status
   * @throws TransportException when no complete response arrived, saying whether the request may
   *     have reached the server; the client retries from those facts
   */
  TransportResponse send(TransportRequest request) throws TransportException;
}
