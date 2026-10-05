package com.howtodoinjava.hibernate.secondlevelcache;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/** Only the Jakarta Persistence annotation: cached with the provider's default strategy. */
@Entity
@Cacheable
public class Language {

  @Id
  @GeneratedValue
  Long id;

  String name;

  protected Language() {
  }

  Language(String name) {
    this.name = name;
  }
}
