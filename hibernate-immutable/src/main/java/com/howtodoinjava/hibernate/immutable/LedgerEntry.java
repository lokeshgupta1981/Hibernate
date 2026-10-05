package com.howtodoinjava.hibernate.immutable;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
public class LedgerEntry {

  @Id
  @GeneratedValue
  private Long id;

  private String description;
  private BigDecimal amount;
  private LocalDateTime postedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  private Journal journal;

  protected LedgerEntry() {
  }

  public LedgerEntry(String description, String amount, LocalDateTime postedAt) {
    this.description = description;
    this.amount = new BigDecimal(amount);
    this.postedAt = postedAt;
  }

  void setJournal(Journal journal) {
    this.journal = journal;
  }

  // Only here to show that Hibernate ignores the change
  public void setDescription(String description) {
    this.description = description;
  }

  public Long getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public LocalDateTime getPostedAt() {
    return postedAt;
  }

  public Journal getJournal() {
    return journal;
  }

  @Override
  public String toString() {
    return description + " " + amount;
  }
}
