package com.howtodoinjava.hibernate.searchboot;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/** Without the MassIndexer, the rows inserted by data.sql are in the database but not in the index. */
@SpringBootTest(properties = {
    "wine.search.reindex-on-startup=false",
    "spring.datasource.url=jdbc:h2:mem:wines-not-indexed",
    "spring.jpa.properties.hibernate.search.backend.directory.type=local-heap",
    // Without the spring.jpa.properties prefix, Spring Boot never passes these to Hibernate Search
    "hibernate.search.backend.directory.type=local-filesystem",
    "hibernate.search.backend.directory.root=target/ignored-index"
})
class StartupWithoutIndexingTest {

  @Autowired
  WineSearchService wineSearchService;

  @Autowired
  WineRepository wineRepository;

  @Autowired
  ApplicationContext context;

  @Test
  void dataSqlRowsAreNotSearchable() {
    assertThat(context.getBeanNamesForType(IndexRebuilder.class)).isEmpty();
    assertThat(wineRepository.count()).isEqualTo(8);
    assertThat(wineSearchService.search("", null, 0, 10, WineSort.RELEVANCE).total()).isZero();
    assertThat(wineSearchService.search("cherry", null, 0, 10, WineSort.RELEVANCE).total()).isZero();
  }

  @Test
  void propertiesWithoutSpringJpaPrefixAreIgnored() {
    assertThat(Files.exists(Path.of("target/ignored-index"))).isFalse();
  }
}
