package com.howtodoinjava.hibernate.pagination;

import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.query.KeyedResultList;

public class PaginationDemo {

  public static void main(String[] args) {
    try (EntityManagerFactory emf = Database.create(true)) {
      Database.seed(emf);

      step("1. Offset pagination: pages 1, 2 and 3 of 5 postings, newest first");
      emf.runInTransaction(em -> {
        for (int page = 1; page <= 3; page++) {
          System.out.println("page " + page + " = " + JobBoard.findPage(em, page, 5));
        }
      });

      step("2. Count query and total pages");
      emf.runInTransaction(em -> {
        PageResult<JobPosting> page = JobBoard.findPageWithTotals(em, 2, 5);
        System.out.println("totalElements = " + page.totalElements() + ", totalPages = " + page.totalPages()
            + ", hasNext = " + page.hasNext());
      });

      step("3. Hibernate getResultCount()");
      emf.runInTransaction(em -> System.out.println("Berlin = " + JobBoard.countWithHibernate(em, "Berlin")));

      step("4. Order by city only (duplicate values)");
      emf.runInTransaction(em -> {
        for (int page = 1; page <= 3; page++) {
          System.out.println("page " + page + " = " + JobBoard.findPageByCity(em, page, 5));
        }
      });

      step("5. Order by city, id");
      emf.runInTransaction(em -> {
        for (int page = 1; page <= 3; page++) {
          System.out.println("page " + page + " = " + JobBoard.findPageByCityStable(em, page, 5));
        }
      });

      step("6. Hibernate Page API: Page.page(5, 1)");
      emf.runInTransaction(em -> System.out.println(JobBoard.findPageWithPageApi(em, 1, 5)));

      step("7. Manual keyset pagination by id");
      emf.runInTransaction(em -> {
        var first = JobBoard.findNextById(em, null, 5);
        System.out.println("first = " + first);
        Long lastId = first.getLast().getId();
        System.out.println("lastId = " + lastId + ", next = " + JobBoard.findNextById(em, lastId, 5));
      });

      step("8. Hibernate KeyedPage and KeyedResultList");
      emf.runInTransaction(em -> {
        KeyedResultList<JobPosting> first = JobBoard.findKeyedPage(em, JobBoard.firstKeyedPage(5));
        System.out.println("first = " + first.getResultList() + ", next key = " + first.getNextPage().getKey());
        KeyedResultList<JobPosting> second = JobBoard.findKeyedPage(em, first.getNextPage());
        System.out.println("second = " + second.getResultList());
        KeyedResultList<JobPosting> third = JobBoard.findKeyedPage(em, second.getNextPage());
        System.out.println("third = " + third.getResultList() + ", isLastPage = " + third.isLastPage());
      });

      step("9. Criteria API page and count");
      emf.runInTransaction(em -> {
        System.out.println("Austin page 1 = " + JobBoard.findPageWithCriteria(em, "Austin", 1, 3));
        System.out.println("Austin count = " + JobBoard.countWithCriteria(em, "Austin"));
      });

      step("10. join fetch with setMaxResults on Hibernate 7.4 and H2");
      emf.runInTransaction(em -> {
        var companies = JobBoard.findCompaniesWithPostings(em, 1, 2);
        System.out.println(companies + " postings = " + companies.getFirst().getPostings().size());
      });

      step("11. Two-query fix");
      emf.runInTransaction(em -> {
        var companies = JobBoard.findCompaniesWithPostingsTwoQueries(em, 1, 2);
        System.out.println(companies + " postings = " + companies.getFirst().getPostings().size());
      });

      step("12. ScrollableResults and getResultStream()");
      emf.runInTransaction(em -> {
        System.out.println("scroll total = " + JobBoard.totalSalaryWithScroll(em));
        System.out.println("stream total = " + JobBoard.totalSalaryWithStream(em));
      });

      step("13. LIMIT and OFFSET in HQL, and a native query");
      emf.runInTransaction(em -> {
        System.out.println(JobBoard.findWithHqlLimit(em));
        System.out.println(JobBoard.findTitlesWithNativeQuery(em, 2, 5));
      });

      step("14. A new posting arrives between page 1 and page 2");
      var firstOffset = emf.callInTransaction(em -> JobBoard.findPage(em, 1, 5));
      var firstKeyed = emf.callInTransaction(em -> JobBoard.findKeyedPage(em, JobBoard.firstKeyedPage(5)));
      emf.runInTransaction(em -> em.persist(
          new JobPosting("Java Architect", "Austin", LocalDate.of(2026, 9, 13), new BigDecimal("120000"))));
      emf.runInTransaction(em -> {
        System.out.println("offset page 1 = " + firstOffset);
        System.out.println("offset page 2 = " + JobBoard.findPage(em, 2, 5));
        System.out.println("keyset page 1 = " + firstKeyed.getResultList());
        System.out.println("keyset page 2 = " + JobBoard.findKeyedPage(em, firstKeyed.getNextPage()).getResultList());
      });
    }

    for (boolean fail : new boolean[] {false, true}) {
      step(fail
          ? "16. Same query with fail_on_pagination_over_collection_fetch = true"
          : "15. join fetch with setMaxResults when the database cannot page a subquery (Hibernate 7.3 and older)");
      try (EntityManagerFactory emf = Database.createWithoutOffsetInSubquery(true, fail)) {
        Database.seed(emf);
        emf.runInTransaction(em -> {
          try {
            System.out.println(JobBoard.findCompaniesWithPostings(em, 1, 2));
          } catch (RuntimeException e) {
            System.out.println(e.getClass().getName() + ": " + e.getMessage());
          }
        });
      }
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
  }
}
