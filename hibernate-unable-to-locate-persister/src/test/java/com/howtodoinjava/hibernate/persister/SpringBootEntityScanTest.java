package com.howtodoinjava.hibernate.persister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.howtodoinjava.hibernate.persister.boot.fixed.FixedFerryApp;
import com.howtodoinjava.hibernate.persister.boot.fixed.FixedFerryRouteRepository;
import com.howtodoinjava.hibernate.persister.boot.repository.FerryRepositoryApp;
import com.howtodoinjava.hibernate.persister.boot.scanmissing.FerryBootApp;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

// Cause 6: Spring Boot 4.1.1 scans entities only below the package of the @SpringBootApplication class
class SpringBootEntityScanTest {

  private static ConfigurableApplicationContext start(Class<?> app) {
    return new SpringApplicationBuilder(app)
        .web(WebApplicationType.NONE)
        .logStartupInfo(false)
        .run();
  }

  @Test
  void entityOutsideScannedPackagesFailsAtPersist() {
    try (ConfigurableApplicationContext ctx = start(FerryBootApp.class)) {
      EntityManagerFactory emf = ctx.getBean(EntityManagerFactory.class);
      IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
          () -> emf.runInTransaction(em -> em.persist(new FerryRoute("Helsinki", "Tallinn", 120))));
      assertEquals("Unknown entity type 'com.howtodoinjava.hibernate.persister.FerryRoute'"
          + " ('FerryRoute' does not belong to this persistence unit)", e.getMessage());
    }
  }

  @Test
  void repositoryForUnscannedEntityFailsAtStartup() {
    BeanCreationException e = assertThrows(BeanCreationException.class, () -> start(FerryRepositoryApp.class));
    Throwable root = e.getMostSpecificCause();
    assertInstanceOf(IllegalArgumentException.class, root);
    assertEquals("Not a managed type: class com.howtodoinjava.hibernate.persister.FerryRoute", root.getMessage());
  }

  @Test
  void entityScanFixesIt() {
    try (ConfigurableApplicationContext ctx = start(FixedFerryApp.class)) {
      FixedFerryRouteRepository repository = ctx.getBean(FixedFerryRouteRepository.class);
      repository.save(new FerryRoute("Helsinki", "Tallinn", 120));
      assertEquals(1, repository.count());
    }
  }
}
