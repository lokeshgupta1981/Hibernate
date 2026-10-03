package com.howtodoinjava.hibernate.sorting;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.Map;
import org.hibernate.query.Order;
import org.hibernate.query.SortDirection;
import org.hibernate.query.specification.SelectionSpecification;

/** Every sorting query from the article. The demo prints their SQL, the test checks their results. */
public final class TrailQueries {

  private TrailQueries() {
  }

  // ---------- JPQL / HQL ----------

  public static List<Trail> hql(EntityManager em, String query) {
    return em.createQuery(query, Trail.class).getResultList();
  }

  // ---------- Criteria API ----------

  /** order by region asc nulls last, lengthKm desc */
  public static List<Trail> criteriaByRegionThenLength(EntityManager em) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Trail> query = cb.createQuery(Trail.class);
    Root<Trail> trail = query.from(Trail.class);
    query.select(trail)
        .orderBy(cb.asc(trail.get("region"), Nulls.LAST),
                 cb.desc(trail.get("lengthKm")));
    return em.createQuery(query).getResultList();
  }

  /** order by lower(name) */
  public static List<Trail> criteriaIgnoringCase(EntityManager em) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Trail> query = cb.createQuery(Trail.class);
    Root<Trail> trail = query.from(Trail.class);
    query.select(trail).orderBy(cb.asc(cb.lower(trail.get("name"))));
    return em.createQuery(query).getResultList();
  }

  // ---------- Dynamic sort field from a request parameter ----------

  /** Request parameter -> entity attribute. Anything else is rejected. */
  public static final Map<String, String> SORTABLE = Map.of(
      "name", "name",
      "region", "region",
      "length", "lengthKm",
      "difficulty", "difficulty");

  /** Criteria API version: the attribute name comes from the allow list, never from the request. */
  public static List<Trail> findAll(EntityManager em, String sortBy, boolean descending) {
    String attribute = SORTABLE.get(sortBy);
    if (attribute == null) {
      throw new IllegalArgumentException("Cannot sort by: " + sortBy);
    }
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Trail> query = cb.createQuery(Trail.class);
    Root<Trail> trail = query.from(Trail.class);
    query.select(trail).orderBy(
        descending ? cb.desc(trail.get(attribute)) : cb.asc(trail.get(attribute)),
        cb.asc(trail.get("id")));
    return em.createQuery(query).getResultList();
  }

  /** Hibernate 7 version: SelectionSpecification.sort() with org.hibernate.query.Order. */
  public static List<Trail> findAllSpec(EntityManager em, String sortBy, boolean descending) {
    String attribute = SORTABLE.get(sortBy);
    if (attribute == null) {
      throw new IllegalArgumentException("Cannot sort by: " + sortBy);
    }
    SortDirection direction = descending ? SortDirection.DESCENDING : SortDirection.ASCENDING;
    return SelectionSpecification.create(Trail.class, "from Trail")
        .sort(Order.by(Trail.class, attribute, direction))
        .sort(Order.asc(Trail.class, "id"))
        .createQuery(em)
        .getResultList();
  }

  /** Hibernate 7 Order with ignoringCase() and nulls placement. */
  public static List<Trail> specIgnoringCase(EntityManager em) {
    return SelectionSpecification.create(Trail.class, "from Trail")
        .sort(Order.asc(Trail.class, "name").ignoringCase())
        .createQuery(em)
        .getResultList();
  }

  public static List<Trail> specNullsLast(EntityManager em) {
    return SelectionSpecification.create(Trail.class, "from Trail")
        .sort(Order.by(Trail.class, "region", SortDirection.ASCENDING, Nulls.LAST))
        .sort(Order.asc(Trail.class, "name"))
        .createQuery(em)
        .getResultList();
  }

  /** WRONG: concatenates the request parameter into the query. Only here to show the risk. */
  public static List<Trail> unsafe(EntityManager em, String sortBy) {
    return em.createQuery("from Trail t order by t." + sortBy, Trail.class).getResultList();
  }

  public static List<String> names(List<Trail> trails) {
    return trails.stream().map(Trail::getName).toList();
  }
}
