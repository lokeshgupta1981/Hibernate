package com.howtodoinjava.hibernate.validatorcdi;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Year;

@Entity
public class TaxFiling {

  @Id
  @GeneratedValue
  private Long id;

  @NotBlank
  @RegisteredTaxpayer
  private String taxpayerId;

  @NotNull
  @PastOrPresent
  @Column(name = "tax_year")
  private Year year;

  @NotNull
  @PositiveOrZero
  private BigDecimal income;

  @NotNull
  @PositiveOrZero
  private BigDecimal deductions;

  protected TaxFiling() {
  }

  public TaxFiling(String taxpayerId, int year, String income, String deductions) {
    this.taxpayerId = taxpayerId;
    this.year = Year.of(year);
    this.income = new BigDecimal(income);
    this.deductions = new BigDecimal(deductions);
  }

  public Long getId() {
    return id;
  }

  public String getTaxpayerId() {
    return taxpayerId;
  }

  public Year getYear() {
    return year;
  }

  public BigDecimal getIncome() {
    return income;
  }

  public BigDecimal getDeductions() {
    return deductions;
  }

  @Override
  public String toString() {
    return "TaxFiling[" + taxpayerId + ", " + year + ", " + income + ", " + deductions + "]";
  }
}
