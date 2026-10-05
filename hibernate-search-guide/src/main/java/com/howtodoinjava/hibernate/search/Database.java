package com.howtodoinjava.hibernate.search;

import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  public static EntityManagerFactory create(boolean showSql) {
    return new HibernatePersistenceConfiguration("podcast-search")
        .managedClasses(Episode.class)
        .jdbcUrl("jdbc:h2:mem:podcast;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        // Hibernate Search: Lucene backend, index kept in memory for the demo
        .property("hibernate.search.backend.directory.type", "local-heap")
        .property("hibernate.search.backend.lucene_version", "LATEST")
        .property("hibernate.search.backend.analysis.configurer",
            "class:" + EnglishAnalysisConfigurer.class.getName())
        .createEntityManagerFactory();
  }

  public static List<Episode> episodes() {
    return List.of(
        new Episode("Testing Spring Boot Apps",
            "We write unit tests and integration tests for a Spring Boot service.",
            "Lokesh", LocalDate.of(2026, 1, 10), 42),
        new Episode("Virtual Threads in Java 25",
            "How virtual threads run blocking code without holding platform threads.",
            "Anna", LocalDate.of(2026, 2, 14), 35),
        new Episode("Hibernate Performance",
            "Batch fetching, thread pools and the latest query hints.",
            "Lokesh", LocalDate.of(2026, 3, 3), 50),
        new Episode("Code Reviews",
            "What we look for in a pull request, and how to test a change before merging.",
            "Ravi", LocalDate.of(2026, 4, 21), 28),
        new Episode("Records and Pattern Matching",
            "Records, sealed types and pattern matching in switch.",
            "Anna", LocalDate.of(2026, 5, 30), 31));
  }
}
