package com.howtodoinjava.hibernate.callbacks;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@EntityListeners({ClaimValidationListener.class, ClaimNotificationListener.class})
public class ExpenseClaim {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String description;
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  private ClaimStatus status;

  private LocalDateTime submittedAt;
  private LocalDateTime updatedAt;

  @Transient
  private boolean loaded;

  protected ExpenseClaim() {
  }

  public ExpenseClaim(String description, BigDecimal amount) {
    this.description = description;
    this.amount = amount;
  }

  @PrePersist
  void onSubmit() {
    CallbackLog.add("ExpenseClaim @PrePersist id=" + id);
    submittedAt = LocalDateTime.now();
    if (status == null) {
      status = ClaimStatus.SUBMITTED;
    }
  }

  @PostPersist
  void afterSubmit() {
    CallbackLog.add("ExpenseClaim @PostPersist id=" + id);
  }

  @PreUpdate
  void onUpdate() {
    CallbackLog.add("ExpenseClaim @PreUpdate");
    updatedAt = LocalDateTime.now();
  }

  @PostUpdate
  void afterUpdate() {
    CallbackLog.add("ExpenseClaim @PostUpdate");
  }

  @PreRemove
  void onRemove() {
    CallbackLog.add("ExpenseClaim @PreRemove");
  }

  @PostRemove
  void afterRemove() {
    CallbackLog.add("ExpenseClaim @PostRemove");
  }

  @PostLoad
  void afterLoad() {
    CallbackLog.add("ExpenseClaim @PostLoad");
    loaded = true;
  }

  public Long getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public ClaimStatus getStatus() {
    return status;
  }

  public void setStatus(ClaimStatus status) {
    this.status = status;
  }

  public LocalDateTime getSubmittedAt() {
    return submittedAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public boolean isLoaded() {
    return loaded;
  }
}
