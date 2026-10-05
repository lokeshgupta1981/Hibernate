package com.howtodoinjava.hibernate.callbacks;

import jakarta.persistence.Entity;
import jakarta.persistence.ExcludeDefaultListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** SEQUENCE ids and Hibernate's @CreationTimestamp / @UpdateTimestamp, for comparison. */
@Entity
@ExcludeDefaultListeners
public class TravelClaim {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  private String description;
  private BigDecimal amount;

  @CreationTimestamp
  private LocalDateTime submittedAt;

  @UpdateTimestamp
  private LocalDateTime updatedAt;

  protected TravelClaim() {
  }

  public TravelClaim(String description, BigDecimal amount) {
    this.description = description;
    this.amount = amount;
  }

  @PrePersist
  void onSubmit() {
    CallbackLog.add("TravelClaim @PrePersist id=" + id + " submittedAt=" + submittedAt);
  }

  @PostPersist
  void afterSubmit() {
    CallbackLog.add("TravelClaim @PostPersist id=" + id + " submittedAt=" + (submittedAt != null ? "set" : null));
  }

  public Long getId() {
    return id;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public LocalDateTime getSubmittedAt() {
    return submittedAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
