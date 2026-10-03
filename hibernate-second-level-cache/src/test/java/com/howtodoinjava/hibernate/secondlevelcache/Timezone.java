package com.howtodoinjava.hibernate.secondlevelcache;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/** READ_ONLY without @Immutable: Hibernate warns at startup and fails on update. */
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class Timezone {

  @Id
  @GeneratedValue
  Long id;

  String name;

  protected Timezone() {
  }

  Timezone(String name) {
    this.name = name;
  }
}
