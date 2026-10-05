package com.howtodoinjava.hibernate.nativeupdate;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.QueryHint;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.jpa.HibernateHints;

@Entity
@Table(name = "plan")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@NamedNativeQuery(name = "Plan.deactivateCheaperThan",
    query = "update plan set active = false where active = true and monthly_price < :minPrice")
@NamedNativeQuery(name = "Plan.setPrice",
    query = "update plan set monthly_price = :price where name = :name",
    hints = @QueryHint(name = HibernateHints.HINT_NATIVE_SPACES, value = "plan"))
public class Plan {

  // counts @PreUpdate calls, to show which update styles run entity callbacks
  public static int preUpdateCalls;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  @Column(name = "monthly_price")
  private BigDecimal monthlyPrice;

  private boolean active;

  protected Plan() {
  }

  public Plan(String name, String monthlyPrice, boolean active) {
    this.name = name;
    this.monthlyPrice = new BigDecimal(monthlyPrice);
    this.active = active;
  }

  @PreUpdate
  void beforeUpdate() {
    preUpdateCalls++;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public BigDecimal getMonthlyPrice() {
    return monthlyPrice;
  }

  public void setMonthlyPrice(BigDecimal monthlyPrice) {
    this.monthlyPrice = monthlyPrice;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  @Override
  public String toString() {
    return name + " " + monthlyPrice + (active ? " active" : " inactive");
  }
}
