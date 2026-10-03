package com.howtodoinjava.hibernate.emf;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.Map;

/** One EntityManagerFactory for the whole application, created on first use. */
public final class CoffeeShopDatabase {

  private static final EntityManagerFactory EMF = build();

  private CoffeeShopDatabase() {
  }

  private static EntityManagerFactory build() {
    EntityManagerFactory emf = Persistence.createEntityManagerFactory("coffee-shop",
        Map.of("jakarta.persistence.jdbc.url", "jdbc:h2:mem:menu-app"));
    Runtime.getRuntime().addShutdownHook(new Thread(emf::close));
    return emf;
  }

  public static EntityManagerFactory emf() {
    return EMF;
  }
}
