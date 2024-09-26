package com.howtodoinjava.demo.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Slf4j
public class BuildLuceneIndexOnStartupListener implements ApplicationListener<ApplicationReadyEvent> {

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {

    log.info("Started Initializing Indexes");
    MassIndexer massIndexer = Search.session( entityManager ).massIndexer();

    massIndexer.idFetchSize(100)
        .batchSizeToLoadObjects(25)
        .threadsToLoadObjects(4);

    try {
      massIndexer.startAndWait();
    } catch (InterruptedException e) {
      log.warn("Failed to load data from database");
      Thread.currentThread().interrupt();
    }

    log.info("Completed Indexing");
  }
}
