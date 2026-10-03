package com.howtodoinjava.hibernate.searchboot;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.search.mapper.orm.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Rows inserted by data.sql (or by any other program) never reach the index.
 * When the application is ready, we rebuild the Wine index from the database.
 */
@Component
@ConditionalOnBooleanProperty(name = "wine.search.reindex-on-startup", matchIfMissing = true)
public class IndexRebuilder {

  private static final Logger log = LoggerFactory.getLogger(IndexRebuilder.class);

  private final EntityManagerFactory entityManagerFactory;

  public IndexRebuilder(EntityManagerFactory entityManagerFactory) {
    this.entityManagerFactory = entityManagerFactory;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void rebuildIndex() throws InterruptedException {
    log.info("Mass indexing started");
    long start = System.currentTimeMillis();
    try (EntityManager em = entityManagerFactory.createEntityManager()) {
      Search.session(em).massIndexer(Wine.class)
          .threadsToLoadObjects(2)
          .batchSizeToLoadObjects(25)
          .startAndWait();
    }
    log.info("Mass indexing finished in {} ms", System.currentTimeMillis() - start);
  }
}
