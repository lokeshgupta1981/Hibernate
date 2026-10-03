package com.howtodoinjava.hibernate.callbacks;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.math.BigDecimal;

public class ClaimValidationListener {

  @PrePersist
  @PreUpdate
  void validate(ExpenseClaim claim) {
    CallbackLog.add("ClaimValidationListener validate");
    if (claim.getAmount() == null || claim.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Claim amount must be positive: " + claim.getAmount());
    }
  }
}
