package com.howtodoinjava.hibernate.sorting;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  public static EntityManagerFactory create(boolean showSql) {
    return configuration(showSql).createEntityManagerFactory();
  }

  /** Same database, with a default placement for nulls in every ORDER BY ("first" or "last"). */
  public static EntityManagerFactory create(boolean showSql, String defaultNullOrdering) {
    return configuration(showSql)
        .property(QuerySettings.DEFAULT_NULL_ORDERING, defaultNullOrdering)
        .createEntityManagerFactory();
  }

  private static HibernatePersistenceConfiguration configuration(boolean showSql) {
    return new HibernatePersistenceConfiguration("sorting")
        .managedClasses(Trail.class, TrailReview.class)
        .jdbcUrl("jdbc:h2:mem:trails;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false);
  }

  /** Five trails, three reviews, four waypoints and three tags. Returns the id of Eagle Peak. */
  public static Long load(EntityManagerFactory emf) {
    return emf.callInTransaction(em -> {
      Trail eagle = new Trail("Eagle Peak", "Alps", 12.5, "expert");
      eagle.addReview(new TrailReview("Lokesh", 5));
      eagle.addReview(new TrailReview("Maria", 3));
      eagle.addReview(new TrailReview("alex", 5));
      eagle.getWaypoints().addAll(List.of("Parking", "Bridge", "Hut", "Summit"));
      eagle.getTags().addAll(List.of("views", "Forest", "lake"));
      em.persist(eagle);
      eagle.getReviews().forEach(em::persist);

      em.persist(new Trail("Lake Loop", "Alps", 5.0, "beginner"));
      em.persist(new Trail("Pine Ridge", null, 8.2, "intermediate"));
      em.persist(new Trail("bear creek", "Rockies", 6.4, "beginner"));
      em.persist(new Trail("Canyon Rim", "Rockies", 15.0, "expert"));
      return eagle.getId();
    });
  }
}
