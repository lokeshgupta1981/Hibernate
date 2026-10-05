package com.howtodoinjava.hibernate.callbacks;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;

public class ClaimNotificationListener {

  @PostPersist
  void submitted(ExpenseClaim claim) {
    CallbackLog.add("ClaimNotificationListener notify manager: claim " + claim.getId() + " submitted");
  }

  @PostUpdate
  void changed(ExpenseClaim claim) {
    CallbackLog.add("ClaimNotificationListener notify employee: claim " + claim.getId() + " is " + claim.getStatus());
  }
}
