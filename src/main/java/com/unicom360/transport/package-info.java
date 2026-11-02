/**
 * The HTTP transport seam. The default, {@link com.unicom360.transport.JdkHttpTransport}, runs on
 * {@code java.net.http.HttpClient}; implement {@link com.unicom360.transport.Transport} to use
 * another HTTP library, a proxy or a fake in your tests.
 */
package com.unicom360.transport;
