package com.howtodoinjava.hibernate.validatorcdi;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Set;

/**
 * Stands in for a lookup in a real taxpayer database.
 */
@ApplicationScoped
public class TaxpayerRegistry {

  private final Set<String> taxpayers = Set.of("lokesh", "alex", "priya");

  public boolean isRegistered(String taxpayerId) {
    return taxpayers.contains(taxpayerId);
  }
}
