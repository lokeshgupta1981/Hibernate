package com.howtodoinjava.hibernate.secondlevelcache;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/** No cache annotation: never cached with the default ENABLE_SELECTIVE mode. */
@Entity
public class Ocean {

  @Id
  @GeneratedValue
  Long id;

  String name;

  protected Ocean() {
  }

  Ocean(String name) {
    this.name = name;
  }
}
