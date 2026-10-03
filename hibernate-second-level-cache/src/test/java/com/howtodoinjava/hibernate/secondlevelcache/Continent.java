package com.howtodoinjava.hibernate.secondlevelcache;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/** Only the Hibernate annotation, without @Cacheable. */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Continent {

  @Id
  @GeneratedValue
  Long id;

  String name;

  protected Continent() {
  }

  Continent(String name) {
    this.name = name;
  }
}
