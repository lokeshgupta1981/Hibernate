package com.howtodoinjava.hibernate.ehcacheconfig;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Language {

  @Id
  @GeneratedValue
  private Long id;

  private String code;
  private String name;

  @OneToMany(mappedBy = "language")
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private List<Translation> translations = new ArrayList<>();

  protected Language() {
  }

  public Language(String code, String name) {
    this.code = code;
    this.name = name;
  }

  public Translation translate(String key, String text) {
    Translation translation = new Translation(key, text, this);
    translations.add(translation);
    return translation;
  }

  public Long getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<Translation> getTranslations() {
    return translations;
  }

  @Override
  public String toString() {
    return code + " (" + name + ")";
  }
}
