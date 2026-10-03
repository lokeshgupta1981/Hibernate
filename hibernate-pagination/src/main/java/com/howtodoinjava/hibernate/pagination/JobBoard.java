package com.howtodoinjava.hibernate.pagination;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.KeyedPage;
import org.hibernate.query.KeyedResultList;
import org.hibernate.query.Order;
import org.hibernate.query.Page;

/** Every paging technique from the article, as small methods that run inside a transaction. */
public final class JobBoard {

  private JobBoard() {
  }

  // 1. Offset pagination: newest postings first, page numbers start at 1
  public static List<JobPosting> findPage(EntityManager em, int pageNumber, int pageSize) {
    return em.createQuery("from JobPosting p order by p.postedOn desc, p.id desc", JobPosting.class)
        .setFirstResult((pageNumber - 1) * pageSize)
        .setMaxResults(pageSize)
        .getResultList();
  }

  // 2. Count query for the total number of rows
  public static long countPostings(EntityManager em) {
    return em.createQuery("select count(p) from JobPosting p", Long.class)
        .getSingleResult();
  }

  // 3. A page plus its totals, for a pager UI
  public static PageResult<JobPosting> findPageWithTotals(EntityManager em, int pageNumber, int pageSize) {
    long total = countPostings(em);
    List<JobPosting> content = findPage(em, pageNumber, pageSize);
    return new PageResult<>(content, pageNumber, pageSize, total);
  }

  // 4. Hibernate's own count: getResultCount() ignores the order and the limit
  public static long countWithHibernate(EntityManager em, String city) {
    return em.unwrap(Session.class)
        .createSelectionQuery("from JobPosting p where p.city = :city order by p.postedOn desc", JobPosting.class)
        .setParameter("city", city)
        .getResultCount();
  }

  // 5. Ordering by a column with duplicate values: pages can overlap
  public static List<JobPosting> findPageByCity(EntityManager em, int pageNumber, int pageSize) {
    return em.createQuery("from JobPosting p order by p.city", JobPosting.class)
        .setFirstResult((pageNumber - 1) * pageSize)
        .setMaxResults(pageSize)
        .getResultList();
  }

  // 6. The fix: add the id as the last sort column
  public static List<JobPosting> findPageByCityStable(EntityManager em, int pageNumber, int pageSize) {
    return em.createQuery("from JobPosting p order by p.city, p.id", JobPosting.class)
        .setFirstResult((pageNumber - 1) * pageSize)
        .setMaxResults(pageSize)
        .getResultList();
  }

  // 7. Hibernate's Page class (page numbers start at 0)
  public static List<JobPosting> findPageWithPageApi(EntityManager em, int pageIndex, int pageSize) {
    return em.unwrap(Session.class)
        .createSelectionQuery("from JobPosting p order by p.postedOn desc, p.id desc", JobPosting.class)
        .setPage(Page.page(pageSize, pageIndex))
        .getResultList();
  }

  // 8. Manual keyset pagination: continue after the last id we showed
  public static List<JobPosting> findNextById(EntityManager em, Long lastId, int pageSize) {
    if (lastId == null) {
      return em.createQuery("from JobPosting p order by p.id desc", JobPosting.class)
          .setMaxResults(pageSize)
          .getResultList();
    }
    return em.createQuery("from JobPosting p where p.id < :lastId order by p.id desc", JobPosting.class)
        .setParameter("lastId", lastId)
        .setMaxResults(pageSize)
        .getResultList();
  }

  // 9. Hibernate key-based pagination with KeyedPage and KeyedResultList
  public static KeyedPage<JobPosting> firstKeyedPage(int pageSize) {
    return Page.first(pageSize).keyedBy(List.of(
        Order.desc(JobPosting.class, "postedOn"),
        Order.desc(JobPosting.class, "id")));
  }

  public static KeyedResultList<JobPosting> findKeyedPage(EntityManager em, KeyedPage<JobPosting> page) {
    return em.unwrap(Session.class)
        .createSelectionQuery("from JobPosting p", JobPosting.class)
        .getKeyedResultList(page);
  }

  // 10. Criteria API: same setFirstResult() and setMaxResults() calls
  public static List<JobPosting> findPageWithCriteria(EntityManager em, String city, int pageNumber, int pageSize) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<JobPosting> query = cb.createQuery(JobPosting.class);
    Root<JobPosting> posting = query.from(JobPosting.class);
    query.select(posting)
        .where(cb.equal(posting.get("city"), city))
        .orderBy(cb.desc(posting.get("postedOn")), cb.desc(posting.get("id")));

    return em.createQuery(query)
        .setFirstResult((pageNumber - 1) * pageSize)
        .setMaxResults(pageSize)
        .getResultList();
  }

  public static long countWithCriteria(EntityManager em, String city) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Long> count = cb.createQuery(Long.class);
    Root<JobPosting> root = count.from(JobPosting.class);
    count.select(cb.count(root)).where(cb.equal(root.get("city"), city));
    return em.createQuery(count).getSingleResult();
  }

  // 11. Paging a join fetch of a collection
  public static List<Company> findCompaniesWithPostings(EntityManager em, int pageNumber, int pageSize) {
    return em.createQuery("select c from Company c join fetch c.postings order by c.name", Company.class)
        .setFirstResult((pageNumber - 1) * pageSize)
        .setMaxResults(pageSize)
        .getResultList();
  }

  // 12. The two-query fix: page the ids, then fetch the collections for those ids
  public static List<Company> findCompaniesWithPostingsTwoQueries(EntityManager em, int pageNumber, int pageSize) {
    List<Long> ids = em.createQuery("select c.id from Company c order by c.name", Long.class)
        .setFirstResult((pageNumber - 1) * pageSize)
        .setMaxResults(pageSize)
        .getResultList();

    return em.createQuery("select c from Company c join fetch c.postings where c.id in :ids order by c.name",
            Company.class)
        .setParameter("ids", ids)
        .getResultList();
  }

  // 13. Streaming every row with ScrollableResults
  public static BigDecimal totalSalaryWithScroll(EntityManager em) {
    BigDecimal total = BigDecimal.ZERO;
    int count = 0;
    try (ScrollableResults<JobPosting> postings = em.unwrap(Session.class)
        .createSelectionQuery("from JobPosting p order by p.id", JobPosting.class)
        .setFetchSize(100)
        .scroll(ScrollMode.FORWARD_ONLY)) {
      while (postings.next()) {
        total = total.add(postings.get().getSalary());
        if (++count % 100 == 0) {
          em.clear();   // free the entities we already processed
        }
      }
    }
    return total;
  }

  // 14. Same with a Jakarta Persistence stream
  public static BigDecimal totalSalaryWithStream(EntityManager em) {
    try (Stream<JobPosting> postings = em.createQuery("from JobPosting p order by p.id", JobPosting.class)
        .getResultStream()) {
      return postings.map(JobPosting::getSalary).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
  }

  // 15. LIMIT and OFFSET written in HQL
  public static List<JobPosting> findWithHqlLimit(EntityManager em) {
    return em.createQuery("from JobPosting p order by p.id limit 5 offset 5", JobPosting.class)
        .getResultList();
  }

  // 16. Native SQL query with the same paging methods
  @SuppressWarnings("unchecked")
  public static List<String> findTitlesWithNativeQuery(EntityManager em, int pageNumber, int pageSize) {
    return em.createNativeQuery("select title from JobPosting order by id", String.class)
        .setFirstResult((pageNumber - 1) * pageSize)
        .setMaxResults(pageSize)
        .getResultList();
  }
}
