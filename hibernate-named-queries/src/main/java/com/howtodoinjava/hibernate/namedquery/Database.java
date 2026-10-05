package com.howtodoinjava.hibernate.namedquery;

import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  public static EntityManagerFactory create(boolean showSql, Class<?>... extraClasses) {
    List<Class<?>> classes = new ArrayList<>(List.of(Room.class, Booking.class));
    classes.addAll(List.of(extraClasses));
    return configuration(showSql)
        .managedClasses(classes)
        .createEntityManagerFactory();
  }

  public static HibernatePersistenceConfiguration configuration(boolean showSql) {
    return new HibernatePersistenceConfiguration("named-queries")
        .mappingFile("META-INF/orm.xml")
        .jdbcUrl("jdbc:h2:mem:hotel;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false);
  }

  public static void seed(EntityManagerFactory emf) {
    emf.runInTransaction(em -> {
      Room r101 = new Room(101, "single", "80.00", true);
      Room r102 = new Room(102, "single", "80.00", false);
      Room r201 = new Room(201, "double", "120.00", true);
      Room r202 = new Room(202, "double", "120.00", true);
      Room r301 = new Room(301, "suite", "250.00", false);
      List.of(r101, r102, r201, r202, r301).forEach(em::persist);
      em.persist(new Booking("Lokesh", 3, r102));
      em.persist(new Booking("Alex", 5, r301));
      em.persist(new Booking("Maria", 1, r201));
    });
  }
}
