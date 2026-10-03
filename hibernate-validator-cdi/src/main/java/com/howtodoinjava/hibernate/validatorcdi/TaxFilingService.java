package com.howtodoinjava.hibernate.validatorcdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.executable.ExecutableType;
import jakarta.validation.executable.ValidateOnExecution;

import java.math.BigDecimal;

/**
 * A CDI bean with method constraints. The Hibernate Validator CDI extension adds
 * its ValidationInterceptor to these methods, so no validation code is needed here.
 */
@ApplicationScoped
public class TaxFilingService {

  private String lastReceipt;

  public String submit(@NotNull @Valid TaxFiling filing) {
    lastReceipt = "Filed " + filing.getYear() + " for " + filing.getTaxpayerId();
    return lastReceipt;
  }

  public @PositiveOrZero BigDecimal taxableIncome(TaxFiling filing) {
    return filing.getIncome().subtract(filing.getDeductions());
  }

  // Getters are not validated by default
  public @NotNull String getLastReceipt() {
    return lastReceipt;
  }

  // Same getter, with validation switched on for it
  @ValidateOnExecution(type = ExecutableType.GETTER_METHODS)
  public @NotNull String getCheckedReceipt() {
    return lastReceipt;
  }

  public void reset() {
    lastReceipt = null;
  }
}
