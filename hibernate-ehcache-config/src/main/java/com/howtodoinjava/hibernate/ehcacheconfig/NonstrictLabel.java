package com.howtodoinjava.hibernate.ehcacheconfig;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/** A UI label cached with CacheConcurrencyStrategy.NONSTRICT_READ_WRITE. */
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class NonstrictLabel implements Label {

  @Id
  @GeneratedValue
  private Long id;

  private String text;

  protected NonstrictLabel() {
  }

  public NonstrictLabel(String text) {
    this.text = text;
  }

  @Override
  public Long getId() {
    return id;
  }

  @Override
  public String getText() {
    return text;
  }

  @Override
  public void setText(String text) {
    this.text = text;
  }
}
