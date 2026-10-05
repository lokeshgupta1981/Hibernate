package com.howtodoinjava.hibernate.immutable;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  public static EntityManagerFactory create(boolean showSql) {
    return create(showSql, null, null);
  }

  // updateMode: null (Hibernate default), "allow", "warning" or "exception"
  public static EntityManagerFactory create(boolean showSql, String updateMode, SqlLog sqlLog) {
    HibernatePersistenceConfiguration config = new HibernatePersistenceConfiguration("immutable")
        .managedClasses(Journal.class, LedgerEntry.class, JournalBalance.class)
        .jdbcUrl("jdbc:h2:mem:ledger;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false);
    if (updateMode != null) {
      config.property("hibernate.query.immutable_entity_update_query_handling_mode", updateMode);
    }
    if (sqlLog != null) {
      config.property("hibernate.session_factory.statement_inspector", sqlLog);
    }
    return config.createEntityManagerFactory();
  }

  public static Long seed(EntityManagerFactory emf) {
    return emf.callInTransaction(em -> {
      Journal journal = new Journal("Household", "Lokesh", LocalDate.of(2026, 1, 1));
      journal.post(new LedgerEntry("Coffee beans", "40.00",
          LocalDateTime.of(2026, 10, 1, 9, 0)));
      journal.post(new LedgerEntry("Pancakes", "12.50",
          LocalDateTime.of(2026, 10, 2, 8, 30)));
      em.persist(journal);
      return journal.getId();
    });
  }

  public static String description(EntityManagerFactory emf, Long entryId) {
    return emf.callInTransaction(em -> (String) em
        .createNativeQuery("select description from LedgerEntry where id = ?1")
        .setParameter(1, entryId)
        .getSingleResult());
  }
}
