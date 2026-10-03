package com.howtodoinjava.hibernate.mapping;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * The bicycle mapped with Jakarta Persistence annotations.
 */
@Entity
@Table(name = "bicycle")
public class Bicycle {

  @Id
  @GeneratedValue
  private Long id;

  @Column(nullable = false, length = 50)
  private String model;

  @Column(name = "frame_size")
  private int frameSize;

  @Column(name = "daily_rate", precision = 6, scale = 2)
  private BigDecimal dailyRate;

  protected Bicycle() {
  }

  public Bicycle(String model, int frameSize, BigDecimal dailyRate) {
    this.model = model;
    this.frameSize = frameSize;
    this.dailyRate = dailyRate;
  }

  public Long getId() {
    return id;
  }

  public String getModel() {
    return model;
  }

  public int getFrameSize() {
    return frameSize;
  }

  public BigDecimal getDailyRate() {
    return dailyRate;
  }

  @Override
  public String toString() {
    return model + " (" + frameSize + " cm, " + dailyRate + ")";
  }
}
