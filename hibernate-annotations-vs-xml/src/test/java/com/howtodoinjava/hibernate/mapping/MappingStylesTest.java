package com.howtodoinjava.hibernate.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.List;
import org.hibernate.boot.models.MemberResolutionException;
import org.hibernate.cfg.Configuration;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;
import org.junit.jupiter.api.Test;

class MappingStylesTest {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private static final List<String> BICYCLE_COLUMNS = List.of(
      "daily_rate numeric(6,2)", "frame_size integer", "id bigint not null", "model varchar(50) not null");

  @Test
  void annotationsCreateTheBicycleTable() {
    try (EntityManagerFactory emf = Database.annotations(false)) {
      Long id = emf.callInTransaction(em -> {
        Bicycle city = new Bicycle("City Cruiser", 54, new BigDecimal("12.50"));
        em.persist(city);
        return city.getId();
      });
      Bicycle found = emf.callInTransaction(em -> em.find(Bicycle.class, id));
      assertEquals("City Cruiser (54 cm, 12.50)", found.toString());
      assertEquals(BICYCLE_COLUMNS, Database.columns(emf, "bicycle"));
    }
  }

  @Test
  void ormXmlMapsAPlainClassToTheSameTable() {
    try (EntityManagerFactory emf = Database.ormXml(false)) {
      Long id = emf.callInTransaction(em -> {
        var city = new com.howtodoinjava.hibernate.mapping.plain.Bicycle("City Cruiser", 54, new BigDecimal("12.50"));
        em.persist(city);
        return city.getId();
      });
      var found = emf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.mapping.plain.Bicycle.class, id));
      assertEquals("City Cruiser (54 cm, 12.50)", found.toString());
      assertEquals(BICYCLE_COLUMNS, Database.columns(emf, "bicycle"));
    }
  }

  @Test
  void ormXmlOverridesTableAndOneColumnAndKeepsOtherAnnotations() {
    try (EntityManagerFactory emf = Database.annotationsWithOverride(false)) {
      emf.runInTransaction(em -> em.persist(new Bicycle("City Cruiser", 54, new BigDecimal("12.50"))));
      assertEquals(List.of(), Database.columns(emf, "bicycle"));
      assertEquals(List.of("frame_size integer", "rate_per_day numeric(8,2)", "id bigint not null",
          "model varchar(50) not null"), Database.columns(emf, "rental_bicycle"));
      String rate = emf.callInTransaction(em -> em.createNativeQuery(
          "select rate_per_day from rental_bicycle").getSingleResult().toString());
      assertEquals("12.50", rate);
    }
  }

  @Test
  void metadataCompleteIgnoresEveryAnnotation() {
    try (EntityManagerFactory emf = Database.metadataComplete(false)) {
      emf.runInTransaction(em -> em.persist(new Bicycle("City Cruiser", 54, new BigDecimal("12.50"))));
      assertEquals(List.of("dailyrate numeric(38,2)", "framesize integer not null", "id bigint not null",
          "model varchar(255)"), Database.columns(emf, "rental_bicycle"));
    }
  }

  @Test
  void hbmXmlStillRunsButLogsADeprecationWarning() {
    PrintStream original = System.err;
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    System.setErr(new PrintStream(err, true));
    EntityManagerFactory emf;
    try {
      emf = Database.hbmXml(false);
    } finally {
      System.setErr(original);
    }
    try (emf) {
      emf.runInTransaction(em -> em.persist(
          new com.howtodoinjava.hibernate.mapping.plain.Bicycle("City Cruiser", 54, new BigDecimal("12.50"))));
      assertEquals(BICYCLE_COLUMNS, Database.columns(emf, "bicycle"));
    }
    assertTrue(err.toString().contains(
        "HHH90000028: Support for `<hibernate-mappings/>` is deprecated [RESOURCE : Bicycle.hbm.xml]"), err.toString());
  }

  @Test
  void hbmXmlCanBeTransformedToMappingXmlOnTheFly() {
    try (EntityManagerFactory emf = new Configuration()
        .addResource("Bicycle.hbm.xml")
        .setProperty("hibernate.transform_hbm_xml.enabled", "true")
        .setProperty("hibernate.connection.url", "jdbc:h2:mem:transform;DB_CLOSE_DELAY=-1")
        .setProperty("hibernate.connection.username", "sa")
        .setProperty("hibernate.connection.password", "")
        .setProperty("hibernate.hbm2ddl.auto", "create-drop")
        .buildSessionFactory()) {
      emf.runInTransaction(em -> em.persist(
          new com.howtodoinjava.hibernate.mapping.plain.Bicycle("City Cruiser", 54, new BigDecimal("12.50"))));
      // the experimental transformer drops length="50" from <property name="model">
      assertEquals(List.of("daily_rate numeric(6,2)", "frame_size integer", "id bigint not null",
          "model varchar(255) not null"), Database.columns(emf, "bicycle"));
    }
  }

  @Test
  void hbmXmlAlsoLoadsThroughMappingFile() {
    try (EntityManagerFactory emf = new HibernatePersistenceConfiguration("hbm-mapping-file")
        .mappingFile("Bicycle.hbm.xml")
        .jdbcUrl("jdbc:h2:mem:hbmfile;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .createEntityManagerFactory()) {
      emf.runInTransaction(em -> em.persist(
          new com.howtodoinjava.hibernate.mapping.plain.Bicycle("City Cruiser", 54, new BigDecimal("12.50"))));
      assertEquals(BICYCLE_COLUMNS, Database.columns(emf, "bicycle"));
    }
  }

  @Test
  void ormXmlWithAWrongFieldNameStopsTheStartup() {
    MemberResolutionException e = assertThrows(MemberResolutionException.class, () ->
        new HibernatePersistenceConfiguration("typo")
            .managedClass(Bicycle.class)
            .mappingFile("META-INF/bicycle-typo.xml")
            .jdbcUrl("jdbc:h2:mem:typo;DB_CLOSE_DELAY=-1")
            .jdbcCredentials("sa", "")
            .createEntityManagerFactory());
    assertEquals("Could not locate attribute member - rate (com.howtodoinjava.hibernate.mapping.Bicycle)",
        e.getMessage());
  }
}
