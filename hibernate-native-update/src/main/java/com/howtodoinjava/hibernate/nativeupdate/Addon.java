package com.howtodoinjava.hibernate.nativeupdate;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/** An extra a subscriber can add to a plan, such as a sports channel. Used to show cache invalidation. */
@Entity
@Table(name = "addon")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Addon {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  @Column(name = "monthly_price")
  private BigDecimal monthlyPrice;

  protected Addon() {
  }

  public Addon(String name, String monthlyPrice) {
    this.name = name;
    this.monthlyPrice = new BigDecimal(monthlyPrice);
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public BigDecimal getMonthlyPrice() {
    return monthlyPrice;
  }

  @Override
  public String toString() {
    return name + " " + monthlyPrice;
  }
}
