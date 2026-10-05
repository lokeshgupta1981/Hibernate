package com.howtodoinjava.hibernate.callbacks;

import jakarta.persistence.Entity;
import jakarta.persistence.ExcludeDefaultListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/** A history row that a callback tries to write; used in the "what not to do" examples. */
@Entity
@ExcludeDefaultListeners
public class ClaimHistory {

  @Id
  @GeneratedValue
  private Long id;

  private Long claimId;
  private String action;

  protected ClaimHistory() {
  }

  public ClaimHistory(Long claimId, String action) {
    this.claimId = claimId;
    this.action = action;
  }

  public Long getClaimId() {
    return claimId;
  }

  public String getAction() {
    return action;
  }
}
