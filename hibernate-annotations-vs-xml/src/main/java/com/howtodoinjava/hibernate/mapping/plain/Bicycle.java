package com.howtodoinjava.hibernate.mapping.plain;

import java.math.BigDecimal;

/**
 * The same bicycle as a plain class without any annotation, as if it came from a library jar.
 * Its mapping lives only in XML (bicycle-orm.xml or Bicycle.hbm.xml).
 */
public class Bicycle {

  private Long id;
  private String model;
  private int frameSize;
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
