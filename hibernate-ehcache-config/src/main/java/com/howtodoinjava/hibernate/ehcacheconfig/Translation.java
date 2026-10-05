package com.howtodoinjava.hibernate.ehcacheconfig;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "translations")
public class Translation {

  @Id
  @GeneratedValue
  private Long id;

  @Column(name = "message_key")
  private String key;

  private String text;

  @ManyToOne(fetch = FetchType.LAZY)
  private Language language;

  protected Translation() {
  }

  Translation(String key, String text, Language language) {
    this.key = key;
    this.text = text;
    this.language = language;
  }

  public Long getId() {
    return id;
  }

  public String getKey() {
    return key;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  @Override
  public String toString() {
    return key + "=" + text;
  }
}
