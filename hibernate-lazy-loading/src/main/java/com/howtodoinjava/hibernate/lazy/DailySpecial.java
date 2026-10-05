package com.howtodoinjava.hibernate.lazy;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** The menu item a restaurant offers on one day. Its @ManyToOne keeps the default EAGER fetch type. */
@Entity
public class DailySpecial {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String weekday;

  @ManyToOne                       // EAGER by default
  @JoinColumn(name = "item_id")
  private MenuItem item;

  protected DailySpecial() {
  }

  public DailySpecial(String weekday, MenuItem item) {
    this.weekday = weekday;
    this.item = item;
  }

  public Long getId() {
    return id;
  }

  public String getWeekday() {
    return weekday;
  }

  public MenuItem getItem() {
    return item;
  }
}
