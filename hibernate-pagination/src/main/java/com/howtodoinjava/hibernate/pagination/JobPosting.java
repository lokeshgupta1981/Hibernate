package com.howtodoinjava.hibernate.pagination;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class JobPosting {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String title;
  private String city;
  private LocalDate postedOn;
  private BigDecimal salary;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id")
  private Company company;

  protected JobPosting() {
  }

  public JobPosting(String title, String city, LocalDate postedOn, BigDecimal salary) {
    this.title = title;
    this.city = city;
    this.postedOn = postedOn;
    this.salary = salary;
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getCity() {
    return city;
  }

  public LocalDate getPostedOn() {
    return postedOn;
  }

  public BigDecimal getSalary() {
    return salary;
  }

  public Company getCompany() {
    return company;
  }

  void setCompany(Company company) {
    this.company = company;
  }

  @Override
  public String toString() {
    return title;
  }
}
