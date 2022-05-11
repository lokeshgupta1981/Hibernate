package com.howtodoinjava.basics;

import com.howtodoinjava.basics.entity.Product;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.junit.jupiter.api.*;

import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestLuceneSearch {

  private static SessionFactory sessionFactory = null;

  @BeforeAll
  static void setup() {
    try {
      StandardServiceRegistry standardRegistry
          = new StandardServiceRegistryBuilder()
          .configure("hibernate-test.cfg.xml")
          .build();

      Metadata metadata = new MetadataSources(standardRegistry)
          .addAnnotatedClass(Product.class)
          .getMetadataBuilder()
          .build();

      sessionFactory = metadata
          .getSessionFactoryBuilder()
          .build();

      setupData();
    } catch (Throwable ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  private static void setupData() {
    try (Session session = sessionFactory.openSession()) {
      session.getTransaction().begin();
      //@formatter:off
      session.persist(Product.builder().name("iPhone 7").company("Apple")
        .features("Fingerprint (front-mounted), accelerometer, gyro, proximity, compass, barometer").build());
      session.persist(Product.builder().name("iPhone 8").company("Apple")
        .features("Fingerprint (front-mounted), accelerometer").build());
      session.persist(Product.builder().name("iPad Mini").company("Apple")
        .features("proximity, compass, barometer, Ultra Wideband (UWB) support").build());
      session.persist(Product.builder().name("iPad Air").company("Apple")
        .features("Fingerprint (top-mounted), accelerometer").build());
      session.persist(Product.builder().name("Galaxy").company("Samsung")
        .features("Fingerprint (side-mounted), accelerometer, gyro, compass, Virtual Proximity Sensing").build());
      session.persist(Product.builder().name("Pixel 6").company("Google")
        .features("Fingerprint (under display, optical), accelerometer").build());
      session.persist(Product.builder().name("Pixel 7").company("Google")
        .features("Fingerprint (under display, optical)").build());
      //@formatter:on
      session.getTransaction().commit();
    }
  }

  @Test
  @Order(1)
  public void massIndexingInApplicationStart() throws InterruptedException {
    SearchSession searchSession =
        Search.session(sessionFactory.createEntityManager());

    SearchSchemaManager schemaManager = searchSession.schemaManager();
    schemaManager.createIfMissing();

    MassIndexer indexer = searchSession.massIndexer()
        .threadsToLoadObjects(7);
    indexer.startAndWait();
  }

  @Test
  @Order(2)
  public void persistWillAddToIndexToo() throws InterruptedException {
    try (Session session = sessionFactory.openSession()) {
      session.getTransaction().begin();
      session.persist(Product.builder().name("Pixel 8").company("Google")
          .features("Test Features").build());
      session.getTransaction().commit();
    }
  }

  @Test
  @Order(3)
  public void simpleSearchByMatchingFieldValue() {
    try (Session session = sessionFactory.openSession()) {
      session.getTransaction().begin();
      SearchSession searchSession =
          Search.session(sessionFactory.createEntityManager());

      SearchResult<Product> result = searchSession.search(Product.class)
          .where(f -> f.match()
              .fields("name")
              .matching("iPhone 7"))
          .fetch(10);

      long totalHitCount = result.total().hitCount();
      List<Product> hits = result.hits();
      session.getTransaction().commit();

      Assertions.assertEquals(1, totalHitCount);
      Assertions.assertEquals("iPhone 7", hits.get(0).getName());
    }
  }

  @Test
  @Order(4)
  public void fullTextSearchOnProductFeatures() {
    try (Session session = sessionFactory.openSession()) {
      session.getTransaction().begin();
      SearchSession searchSession =
          Search.session(sessionFactory.createEntityManager());

      SearchResult<Product> result = searchSession.search(Product.class)
          .where(f -> f.terms()
              .fields("features")
              .matchingAll("accelerometer"))
          .fetch(10);

      long totalHitCount = result.total().hitCount();
      List<Product> hits = result.hits();
      session.getTransaction().commit();

      Assertions.assertEquals(5, totalHitCount);
    }
  }

}
