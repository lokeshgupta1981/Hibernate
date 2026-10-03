package com.howtodoinjava.hibernate.softdelete;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/** Test-only: a folder WITHOUT @SoftDelete, to compare lazy loading. */
@Entity
public class PlainFolder {

  @Id
  @GeneratedValue
  Long id;

  String name;

  protected PlainFolder() {
  }

  PlainFolder(String name) {
    this.name = name;
  }
}
