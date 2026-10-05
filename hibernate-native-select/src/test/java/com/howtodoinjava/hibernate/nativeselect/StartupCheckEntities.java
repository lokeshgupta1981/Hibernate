package com.howtodoinjava.hibernate.nativeselect;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQuery;

/** Test-only entities with a mistake in a named query, used to see what Hibernate checks at startup. */
final class StartupCheckEntities {

  private StartupCheckEntities() {
  }

  @Entity
  @NamedNativeQuery(name = "Basket.wrongTable", query = "select * from Produces", resultClass = Basket.class)
  static class Basket {
    @Id
    Long id;
  }

  @Entity
  @NamedQuery(name = "Produce.findAll", query = "select p from Produces p")
  static class Crate {
    @Id
    Long id;
  }
}
