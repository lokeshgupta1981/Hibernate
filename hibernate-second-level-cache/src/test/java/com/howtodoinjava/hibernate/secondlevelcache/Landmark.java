package com.howtodoinjava.hibernate.secondlevelcache;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/** NONSTRICT_READ_WRITE: an update removes the entry instead of replacing it. */
@Entity
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Landmark {

  @Id
  @GeneratedValue
  Long id;

  String name;

  protected Landmark() {
  }

  Landmark(String name) {
    this.name = name;
  }
}
