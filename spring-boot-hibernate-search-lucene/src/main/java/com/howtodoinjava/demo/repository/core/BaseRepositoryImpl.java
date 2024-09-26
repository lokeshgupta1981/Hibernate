package com.howtodoinjava.demo.repository.core;

import jakarta.persistence.EntityManager;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.mapper.orm.Search;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class BaseRepositoryImpl<T, ID extends Serializable>
    extends SimpleJpaRepository<T, ID>
    implements BaseRepository<T, ID> {

  private final EntityManager entityManager;

  public BaseRepositoryImpl(Class<T> domainClass, EntityManager entityManager) {
    super(domainClass, entityManager);
    this.entityManager = entityManager;
  }

  public BaseRepositoryImpl(JpaEntityInformation<T, ID> entityInformation, EntityManager entityManager) {
    super(entityInformation, entityManager);
    this.entityManager = entityManager;
  }

  @Override
  public List<T> fullTextSearch(String text,
                                int offset,
                                int limit,
                                List<String> fields,
                                String sortBy,
                                SortOrder sortOrder) {

    if (text == null || text.isEmpty()) {
      return Collections.emptyList();
    }

    return Search.session(entityManager)
        .search(getDomainClass())
        .where(f -> f.match().fields(fields.toArray(String[]::new)).matching(text))
        .sort(f -> f.field(sortBy).order(sortOrder))
        .fetchHits(offset, limit);
  }

  @Override
  public List<T> fuzzySearch(String text,
                             int offset,
                             int limit,
                             List<String> fields,
                             String sortBy,
                             SortOrder sortOrder) {
    if (text == null || text.isEmpty()) {
      return Collections.emptyList();
    }

    return Search.session(entityManager)
        .search(getDomainClass())
        .where(f -> f.match().fields(fields.toArray(String[]::new)).matching(text).fuzzy())
        .sort(f -> f.field(sortBy).order(sortOrder))
        .fetchHits(offset, limit);
  }

  @Override
  public List<T> wildcardSearch(String pattern,
                                int offset,
                                int limit,
                                List<String> fields,
                                String sortBy,
                                SortOrder sortOrder) {

    if (pattern == null || pattern.isEmpty()) {
      return Collections.emptyList();
    }

    return Search.session(entityManager)
        .search(getDomainClass())
        .where(f -> f.wildcard().fields(fields.toArray(String[]::new)).matching(pattern))
        .sort(f -> f.field(sortBy).order(sortOrder))
        .fetchHits(offset, limit);
  }
}
