package com.howtodoinjava.basics.entity;

import com.howtodoinjava.basics.entity.listners.TransactionListener;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.ZonedDateTime;

@Entity
@Table(name = "TBL_TRANS")
@EntityListeners({TransactionListener.class})
public class TransactionEntity {

  @Transient
  Logger LOG = LoggerFactory.getLogger(TransactionEntity.class);

  @Id
  @Column(name = "id", nullable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column
  private Double amount;

  @Column
  private ZonedDateTime startTS;

  @Column
  private ZonedDateTime endTS;

  @Transient
  private Duration duration;

  @PrePersist
  @PreUpdate
  @PostLoad
  public void updateDuration() {
    LOG.info("TransactionEntity.updateDuration() invoked !!");
    duration = Duration.between(startTS, endTS);
  }

  public Long getId() {return id;}

  public void setId(Long id) {this.id = id;}

  public Double getAmount() {
    return amount;
  }

  public void setAmount(Double amount) {
    this.amount = amount;
  }

  public ZonedDateTime getStartTS() {
    return startTS;
  }

  public void setStartTS(ZonedDateTime startTS) {
    this.startTS = startTS;
  }

  public ZonedDateTime getEndTS() {
    return endTS;
  }

  public void setEndTS(ZonedDateTime endTS) {
    this.endTS = endTS;
  }

  public Duration getDuration() {
    return duration;
  }

  public void setDuration(Duration duration) {
    this.duration = duration;
  }
}
