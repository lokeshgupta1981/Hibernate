package com.howtodoinjava.hibernate.states;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.math.BigDecimal;

@Entity
public class ServiceOrder {

  @Id
  @GeneratedValue
  private Long id;

  private String plateNumber;
  private String issue;
  private String status;
  private BigDecimal cost;

  protected ServiceOrder() {
  }

  public ServiceOrder(String plateNumber, String issue, String status, BigDecimal cost) {
    this.plateNumber = plateNumber;
    this.issue = issue;
    this.status = status;
    this.cost = cost;
  }

  public Long getId() {
    return id;
  }

  public String getPlateNumber() {
    return plateNumber;
  }

  public String getIssue() {
    return issue;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public BigDecimal getCost() {
    return cost;
  }

  public void setCost(BigDecimal cost) {
    this.cost = cost;
  }

  @Override
  public String toString() {
    return "ServiceOrder{id=" + id + ", plateNumber=" + plateNumber + ", issue=" + issue
        + ", status=" + status + ", cost=" + cost + "}";
  }
}
