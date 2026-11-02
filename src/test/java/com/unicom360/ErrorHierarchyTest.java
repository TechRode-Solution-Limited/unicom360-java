package com.unicom360;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.unicom360.errors.ApiConnectionException;
import com.unicom360.errors.ApiException;
import com.unicom360.errors.ApiServerException;
import com.unicom360.errors.ApiStatusException;
import com.unicom360.errors.ApiTimeoutException;
import com.unicom360.errors.AuthenticationException;
import com.unicom360.errors.ConfigurationException;
import com.unicom360.errors.DuplicateReferenceException;
import com.unicom360.errors.InsufficientBundleException;
import com.unicom360.errors.InvalidRequestException;
import com.unicom360.errors.NoValidRecipientsException;
import com.unicom360.errors.RateLimitException;
import com.unicom360.errors.SessionWindowClosedException;
import com.unicom360.errors.SignatureVerificationException;
import com.unicom360.errors.Unicom360Exception;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The hierarchy named in section 8 of the contract. */
class ErrorHierarchyTest {
  @Test
  void theHierarchyIsTheContracts() {
    for (Class<?> child :
        List.of(
            ConfigurationException.class,
            ApiConnectionException.class,
            ApiStatusException.class,
            ApiException.class,
            SignatureVerificationException.class)) {
      assertEquals(Unicom360Exception.class, child.getSuperclass(), child.getName());
    }
    assertEquals(ApiConnectionException.class, ApiTimeoutException.class.getSuperclass());
    for (Class<?> child :
        List.of(
            AuthenticationException.class,
            RateLimitException.class,
            InvalidRequestException.class,
            ApiServerException.class)) {
      assertEquals(ApiStatusException.class, child.getSuperclass(), child.getName());
    }
    for (Class<?> child :
        List.of(
            InsufficientBundleException.class,
            DuplicateReferenceException.class,
            NoValidRecipientsException.class,
            SessionWindowClosedException.class)) {
      assertEquals(ApiException.class, child.getSuperclass(), child.getName());
    }
    assertTrue(RuntimeException.class.isAssignableFrom(Unicom360Exception.class), "unchecked");
  }
}
