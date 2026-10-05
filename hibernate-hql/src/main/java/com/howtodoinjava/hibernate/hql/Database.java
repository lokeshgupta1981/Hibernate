package com.howtodoinjava.hibernate.hql;

import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.JpaComplianceSettings;
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

  /** Same setup, but Hibernate accepts only standard JPQL. */
  public static EntityManagerFactory createJpqlOnly() {
    return configuration(false)
        .property(JpaComplianceSettings.JPA_QUERY_COMPLIANCE, true)
        .createEntityManagerFactory();
  }

  /** Same setup, with the HQL text added as a comment to each SQL statement. */
  public static EntityManagerFactory createWithSqlComments() {
    return configuration(false)
        .property(JdbcSettings.USE_SQL_COMMENTS, true)
        .createEntityManagerFactory();
  }

  private static HibernatePersistenceConfiguration configuration(boolean showSql) {
    return new HibernatePersistenceConfiguration("hql")
        .managedClasses(Gallery.class, Artist.class, Artwork.class, Highlight.class)
        .jdbcUrl("jdbc:h2:mem:museum;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .property(JdbcSettings.STATEMENT_INSPECTOR, SqlLog.class.getName());
  }

  /** Three galleries, four artists and six artworks. */
  public static void seed(EntityManagerFactory emf) {
    emf.runInTransaction(em -> {
      Gallery modern = new Gallery("Modern Wing", 1);
      Gallery sculpture = new Gallery("Sculpture Hall", 0);
      Gallery east = new Gallery("East Room", 2);              // no artworks
      Artist lokesh = new Artist("Lokesh", "India");
      Artist emma = new Artist("Emma", "France");
      Artist hugo = new Artist("Hugo", "France");
      Artist mia = new Artist("Mia", "Japan");                 // no artworks
      for (Object o : new Object[] {modern, sculpture, east, lokesh, emma, hugo, mia}) {
        em.persist(o);
      }
      em.persist(new Artwork("Blue River", 2019, new BigDecimal("1200.00"), Medium.PAINTING, lokesh, modern));
      em.persist(new Artwork("Old Bridge", 2021, new BigDecimal("800.00"), Medium.PAINTING, emma, modern));
      em.persist(new Artwork("Stone Bird", 2015, new BigDecimal("3000.00"), Medium.SCULPTURE, hugo, sculpture));
      em.persist(new Artwork("Night Train", 2023, new BigDecimal("450.00"), Medium.PHOTO, lokesh, modern));
      em.persist(new Artwork("Iron Leaf", 2018, new BigDecimal("2200.00"), Medium.SCULPTURE, emma, sculpture));
      em.persist(new Artwork("Red Fields", 2020, new BigDecimal("950.00"), Medium.PAINTING, hugo, null));
    });
  }
}
