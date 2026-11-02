package com.unicom360;

import com.google.gson.JsonObject;
import com.unicom360.errors.Unicom360Exception;
import com.unicom360.internal.Models;
import com.unicom360.internal.Operation;
import com.unicom360.models.Balance;
import com.unicom360.models.Balances;

/** The balance operations: {@code client.balance()}. */
public final class BalanceService {
  private final Unicom360Client client;

  BalanceService(Unicom360Client client) {
    this.client = client;
  }

  /**
   * The balance of the default bundle ({@code SMS}).
   *
   * @return the balance
   * @throws Unicom360Exception when the call fails or the gateway refuses it
   */
  public Balance get() {
    return get(null, null);
  }

  /**
   * The balance of one bundle.
   *
   * @param bundleType the bundle (see {@link BundleType}), or {@code null} for the default
   * @return the balance
   * @throws Unicom360Exception when the call fails or the gateway refuses it
   */
  public Balance get(String bundleType) {
    return get(bundleType, null);
  }

  /**
   * The balance of one bundle.
   *
   * @param bundleType the bundle (see {@link BundleType}), or {@code null} for the default
   * @param options overrides for this call, or {@code null}
   * @return the balance
   * @throws Unicom360Exception when the call fails or the gateway refuses it
   */
  public Balance get(String bundleType, RequestOptions options) {
    JsonObject body = new JsonObject();
    if (bundleType != null) {
      body.addProperty("bundle_type", bundleType);
    }
    return client.execute(Operation.BALANCE_GET, body, options, Models::balance);
  }

  /**
   * Every bundle's balance.
   *
   * @return the balances
   * @throws Unicom360Exception when the call fails or the gateway refuses it
   */
  public Balances all() {
    return all(null);
  }

  /**
   * Every bundle's balance.
   *
   * @param options overrides for this call, or {@code null}
   * @return the balances
   * @throws Unicom360Exception when the call fails or the gateway refuses it
   */
  public Balances all(RequestOptions options) {
    return client.execute(Operation.BALANCE_ALL, new JsonObject(), options, Models::balances);
  }
}
