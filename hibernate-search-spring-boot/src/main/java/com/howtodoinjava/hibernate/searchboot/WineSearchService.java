package com.howtodoinjava.hibernate.searchboot;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WineSearchService {

  private final EntityManager entityManager;
  private final WineRepository wineRepository;

  public WineSearchService(EntityManager entityManager, WineRepository wineRepository) {
    this.entityManager = entityManager;
    this.wineRepository = wineRepository;
  }

  @Transactional(readOnly = true)
  public WinePage search(String text, String region, int page, int size, WineSort sort) {
    SearchSession searchSession = Search.session(entityManager);

    SearchResult<Wine> result = searchSession.search(Wine.class)
        .where(f -> f.bool().with(b -> {
          b.must(f.matchAll());
          if (text != null && !text.isBlank()) {
            b.must(f.match()
                .field("name").boost(2.0f)
                .fields("grape", "tastingNotes")
                .matching(text));
          }
          if (region != null && !region.isBlank()) {
            b.filter(f.match().field("region").matching(region));
          }
        }))
        .sort(f -> switch (sort) {
          case RELEVANCE -> f.score();
          case NAME -> f.field("name_sort");
          case PRICE_ASC -> f.field("price").asc();
          case PRICE_DESC -> f.field("price").desc();
        })
        .fetch(page * size, size);

    return new WinePage(text, result.total().hitCount(), page, size, result.hits());
  }

  @Transactional
  public Wine save(Wine wine) {
    return wineRepository.save(wine);   // indexed when this transaction commits
  }

  @Transactional(readOnly = true)
  public List<Wine> findAll() {
    return wineRepository.findAll();
  }
}
