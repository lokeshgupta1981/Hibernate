package com.howtodoinjava.hibernate.pagination;

import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  public static EntityManagerFactory create(boolean showSql) {
    return create(showSql, Map.of());
  }

  public static EntityManagerFactory create(boolean showSql, Map<String, Object> settings) {
    HibernatePersistenceConfiguration config = new HibernatePersistenceConfiguration("pagination")
        .managedClasses(Company.class, JobPosting.class)
        .jdbcUrl("jdbc:h2:mem:jobboard;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false);
    settings.forEach(config::property);
    return config.createEntityManagerFactory();
  }

  /** Behaves like a database that cannot use OFFSET/FETCH inside a subquery (and like Hibernate 7.3 or older). */
  public static EntityManagerFactory createWithoutOffsetInSubquery(boolean showSql, boolean failOnInMemoryPaging) {
    return create(showSql, Map.of(
        "hibernate.dialect", NoOffsetInSubqueryH2Dialect.class.getName(),
        "hibernate.query.fail_on_pagination_over_collection_fetch", failOnInMemoryPaging));
  }

  /** Saves 3 companies and 12 job postings, one posting per day from 2026-09-01. */
  public static void seed(EntityManagerFactory emf) {
    String[][] rows = {
        {"Java Developer", "Austin", "95000", "Acme"},
        {"QA Engineer", "Berlin", "70000", "Bluebird"},
        {"Data Analyst", "Pune", "60000", "Cedar"},
        {"DevOps Engineer", "Austin", "105000", "Acme"},
        {"Frontend Developer", "Berlin", "80000", "Bluebird"},
        {"Scrum Master", "Pune", "75000", "Cedar"},
        {"Backend Developer", "Austin", "98000", "Acme"},
        {"Mobile Developer", "Berlin", "85000", "Bluebird"},
        {"Support Engineer", "Pune", "50000", "Cedar"},
        {"Product Manager", "Austin", "110000", "Acme"},
        {"UX Designer", "Berlin", "72000", "Bluebird"},
        {"Tech Writer", "Pune", "55000", "Cedar"}
    };
    emf.runInTransaction(em -> {
      Company acme = new Company("Acme");
      Company bluebird = new Company("Bluebird");
      Company cedar = new Company("Cedar");
      em.persist(acme);
      em.persist(bluebird);
      em.persist(cedar);
      LocalDate day = LocalDate.of(2026, 9, 1);
      for (String[] row : rows) {
        JobPosting posting = new JobPosting(row[0], row[1], day, new BigDecimal(row[2]));
        switch (row[3]) {
          case "Acme" -> acme.addPosting(posting);
          case "Bluebird" -> bluebird.addPosting(posting);
          default -> cedar.addPosting(posting);
        }
        em.persist(posting);
        day = day.plusDays(1);
      }
    });
  }
}
