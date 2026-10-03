package com.howtodoinjava.hibernate.persister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;

// Surefire runs this class without hibernate-scan-jandex on the classpath (see pom.xml)
class ScanningWithoutJandexTest {

  @Test
  void scannedUnitFindsNoEntityWithoutScanJandex() {
    try (EntityManagerFactory emf = Database.fromPersistenceXml("ferry-scanned", false)) {
      assertEquals(0, emf.getMetamodel().getEntities().size());
      IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
          () -> emf.runInTransaction(em -> em.persist(new FerryRoute("Helsinki", "Tallinn", 120))));
      assertEquals("Unknown entity type 'com.howtodoinjava.hibernate.persister.FerryRoute'"
          + " ('FerryRoute' does not belong to this persistence unit)", e.getMessage());
    }
  }
}
