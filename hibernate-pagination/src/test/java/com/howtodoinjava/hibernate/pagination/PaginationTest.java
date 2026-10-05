package com.howtodoinjava.hibernate.pagination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.hibernate.HibernateException;
import org.hibernate.query.KeyedResultList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaginationTest {

  private final SqlCapture sql = new SqlCapture();
  private EntityManagerFactory emf;

  @BeforeEach
  void setUp() {
    emf = Database.create(false, Map.of("hibernate.session_factory.statement_inspector", sql));
    Database.seed(emf);
    sql.clear();
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  private static List<String> titles(List<? extends Object> rows) {
    return rows.stream().map(Object::toString).toList();
  }

  @Test
  void offsetPagesNewestFirst() {
    emf.runInTransaction(em -> {
      assertEquals(List.of("Tech Writer", "UX Designer", "Product Manager", "Support Engineer", "Mobile Developer"),
          titles(JobBoard.findPage(em, 1, 5)));
      assertEquals(List.of("Backend Developer", "Scrum Master", "Frontend Developer", "DevOps Engineer", "Data Analyst"),
          titles(JobBoard.findPage(em, 2, 5)));
      assertEquals(List.of("QA Engineer", "Java Developer"), titles(JobBoard.findPage(em, 3, 5)));
      assertTrue(JobBoard.findPage(em, 4, 5).isEmpty());
    });
    assertTrue(sql.last().endsWith("order by jp1_0.postedOn desc,jp1_0.id desc offset ? rows fetch first ? rows only"));
  }

  @Test
  void countQueryAndTotalPages() {
    PageResult<JobPosting> page = emf.callInTransaction(em -> JobBoard.findPageWithTotals(em, 2, 5));
    assertEquals(12, page.totalElements());
    assertEquals(3, page.totalPages());
    assertTrue(page.hasNext());
    assertEquals("select count(jp1_0.id) from JobPosting jp1_0", sql.statements().getFirst());
    assertEquals(2, sql.statements().size());
    assertEquals(4, new PageResult<>(List.of(), 1, 4, 13).totalPages());
    assertEquals(3, new PageResult<>(List.of(), 1, 4, 12).totalPages());
  }

  @Test
  void hibernateGetResultCountDropsOrderBy() {
    long berlin = emf.callInTransaction(em -> JobBoard.countWithHibernate(em, "Berlin"));
    assertEquals(4, berlin);
    assertEquals("select count(*) from JobPosting jp1_0 where jp1_0.city=?", sql.last());
  }

  @Test
  void hibernateAddsNoOrderByOfItsOwn() {
    emf.runInTransaction(em -> em.createQuery("from JobPosting p", JobPosting.class)
        .setFirstResult(5)
        .setMaxResults(5)
        .getResultList());
    assertEquals("select jp1_0.id,jp1_0.city,jp1_0.company_id,jp1_0.postedOn,jp1_0.salary,jp1_0.title "
        + "from JobPosting jp1_0 offset ? rows fetch first ? rows only", sql.last());
  }

  @Test
  void legacyCriteriaApiIsGone() {
    assertThrows(ClassNotFoundException.class, () -> Class.forName("org.hibernate.Criteria"));
  }

  @Test
  void orderByNonUniqueColumnLosesAndRepeatsRows() {
    // H2 picks rows with the same city in any order, so the result changes from run to run.
    // In about 9 of 10 runs some postings repeat and others are missing; 20 tries make the test reliable.
    int brokenRuns = 0;
    for (int run = 0; run < 20; run++) {
      List<String> all = new ArrayList<>();
      emf.runInTransaction(em -> {
        for (int page = 1; page <= 3; page++) {
          all.addAll(titles(JobBoard.findPageByCity(em, page, 5)));
        }
      });
      assertEquals(12, all.size());
      if (new HashSet<>(all).size() < 12) {
        brokenRuns++;
      }
    }
    assertTrue(brokenRuns > 0);
  }

  @Test
  void orderByCityThenIdReturnsEveryRowOnce() {
    for (int run = 0; run < 20; run++) {
      List<String> all = new ArrayList<>();
      emf.runInTransaction(em -> {
        for (int page = 1; page <= 3; page++) {
          all.addAll(titles(JobBoard.findPageByCityStable(em, page, 5)));
        }
      });
      assertEquals(List.of("Java Developer", "DevOps Engineer", "Backend Developer", "Product Manager", "QA Engineer",
          "Frontend Developer", "Mobile Developer", "UX Designer", "Data Analyst", "Scrum Master",
          "Support Engineer", "Tech Writer"), all);
    }
  }

  @Test
  void pageApiIsZeroBased() {
    assertEquals(List.of("Backend Developer", "Scrum Master", "Frontend Developer", "DevOps Engineer", "Data Analyst"),
        titles(emf.callInTransaction(em -> JobBoard.findPageWithPageApi(em, 1, 5))));
  }

  @Test
  void manualKeysetById() {
    emf.runInTransaction(em -> {
      List<JobPosting> first = JobBoard.findNextById(em, null, 5);
      assertEquals(8L, first.getLast().getId());
      assertEquals(List.of("Backend Developer", "Scrum Master", "Frontend Developer", "DevOps Engineer", "Data Analyst"),
          titles(JobBoard.findNextById(em, 8L, 5)));
    });
    assertTrue(sql.last().endsWith("where jp1_0.id<? order by jp1_0.id desc fetch first ? rows only"));
  }

  @Test
  void keyedPagesWithHibernate() {
    emf.runInTransaction(em -> {
      KeyedResultList<JobPosting> first = JobBoard.findKeyedPage(em, JobBoard.firstKeyedPage(5));
      assertTrue(first.isFirstPage());
      assertEquals(List.of(LocalDate.of(2026, 9, 8), 8L), first.getNextPage().getKey());
      KeyedResultList<JobPosting> second = JobBoard.findKeyedPage(em, first.getNextPage());
      assertEquals(List.of("Backend Developer", "Scrum Master", "Frontend Developer", "DevOps Engineer", "Data Analyst"),
          titles(second.getResultList()));
      assertTrue(sql.last().contains("where jp1_0.postedOn<? or jp1_0.id<? and jp1_0.postedOn=? order by 4 desc,1 desc fetch first ? rows only"));
      KeyedResultList<JobPosting> third = JobBoard.findKeyedPage(em, second.getNextPage());
      assertEquals(List.of("QA Engineer", "Java Developer"), titles(third.getResultList()));
      assertTrue(third.isLastPage());
      assertFalse(third.isFirstPage());
      assertEquals(null, third.getNextPage());
      assertEquals(List.of(List.of(LocalDate.of(2026, 9, 2), 2L), List.of(LocalDate.of(2026, 9, 1), 1L)),
          third.getKeyList());
      assertEquals(titles(second.getResultList()),
          titles(JobBoard.findKeyedPage(em, third.getPreviousPage()).getResultList()));
    });
  }

  @Test
  void newRowShiftsOffsetPagesButNotKeysetPages() {
    List<JobPosting> offsetFirst = emf.callInTransaction(em -> JobBoard.findPage(em, 1, 5));
    KeyedResultList<JobPosting> keyedFirst = emf.callInTransaction(
        em -> JobBoard.findKeyedPage(em, JobBoard.firstKeyedPage(5)));

    emf.runInTransaction(em -> em.persist(
        new JobPosting("Java Architect", "Austin", LocalDate.of(2026, 9, 13), new BigDecimal("120000"))));

    List<String> offsetSecond = titles(emf.callInTransaction(em -> JobBoard.findPage(em, 2, 5)));
    List<String> keyedSecond = titles(emf.callInTransaction(
        em -> JobBoard.findKeyedPage(em, keyedFirst.getNextPage()).getResultList()));

    assertEquals("Mobile Developer", offsetFirst.getLast().getTitle());
    assertEquals(List.of("Mobile Developer", "Backend Developer", "Scrum Master", "Frontend Developer", "DevOps Engineer"),
        offsetSecond);
    assertEquals(List.of("Backend Developer", "Scrum Master", "Frontend Developer", "DevOps Engineer", "Data Analyst"),
        keyedSecond);
  }

  @Test
  void deletedRowHidesAPostingFromOffsetPages() {
    List<String> first = titles(emf.callInTransaction(em -> JobBoard.findPage(em, 1, 5)));
    emf.runInTransaction(em -> em.createQuery("delete from JobPosting p where p.title = 'Tech Writer'").executeUpdate());
    List<String> second = titles(emf.callInTransaction(em -> JobBoard.findPage(em, 2, 5)));
    assertEquals(List.of("Tech Writer", "UX Designer", "Product Manager", "Support Engineer", "Mobile Developer"), first);
    assertEquals(List.of("Scrum Master", "Frontend Developer", "DevOps Engineer", "Data Analyst", "QA Engineer"), second);
    assertFalse(first.contains("Backend Developer"));
  }

  @Test
  void criteriaPageAndCount() {
    emf.runInTransaction(em -> {
      assertEquals(List.of("Product Manager", "Backend Developer", "DevOps Engineer"),
          titles(JobBoard.findPageWithCriteria(em, "Austin", 1, 3)));
      assertTrue(sql.last().endsWith("where jp1_0.city=? order by jp1_0.postedOn desc,jp1_0.id desc offset ? rows fetch first ? rows only"));
      assertEquals(4, JobBoard.countWithCriteria(em, "Austin"));
      assertEquals("select count(jp1_0.id) from JobPosting jp1_0 where jp1_0.city=?", sql.last());
    });
  }

  @Test
  void joinFetchPagingUsesSubqueryOnHibernate74AndH2() {
    emf.runInTransaction(em -> {
      List<Company> companies = JobBoard.findCompaniesWithPostings(em, 1, 2);
      assertEquals(List.of("Acme", "Bluebird"), titles(companies));
      assertEquals(4, companies.get(0).getPostings().size());
      assertEquals(4, companies.get(1).getPostings().size());
    });
    assertEquals(1, sql.statements().size());
    assertTrue(sql.last().contains("from (select c1_0.id,c1_0.name from Company c1_0 where exists("));
    assertTrue(sql.last().contains("order by c1_0.name offset ? rows fetch first ? rows only) c1_0(id,name) join JobPosting"));
  }

  @Test
  void joinFetchPagingInMemoryLogsWarning() {
    SqlCapture oldSql = new SqlCapture();
    PrintStream originalErr = System.err;
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    System.setErr(new PrintStream(err, true));
    try (EntityManagerFactory old = Database.create(false, Map.of(
        "hibernate.dialect", NoOffsetInSubqueryH2Dialect.class.getName(),
        "hibernate.session_factory.statement_inspector", oldSql))) {
      Database.seed(old);
      oldSql.clear();
      List<Company> companies = old.callInTransaction(em -> JobBoard.findCompaniesWithPostings(em, 1, 2));
      assertEquals(List.of("Acme", "Bluebird"), titles(companies));
      assertEquals("select c1_0.id,c1_0.name,p1_0.company_id,p1_0.id,p1_0.city,p1_0.postedOn,p1_0.salary,p1_0.title "
          + "from Company c1_0 join JobPosting p1_0 on c1_0.id=p1_0.company_id order by c1_0.name", oldSql.last());
    } finally {
      System.setErr(originalErr);
    }
    assertTrue(err.toString().contains(
        "WARN org.hibernate.orm.query - HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory"),
        err.toString());
  }

  @Test
  void failOnPaginationOverCollectionFetch() {
    try (EntityManagerFactory old = Database.createWithoutOffsetInSubquery(false, true)) {
      Database.seed(old);
      HibernateException e = assertThrows(HibernateException.class,
          () -> old.runInTransaction(em -> JobBoard.findCompaniesWithPostings(em, 1, 2)));
      assertEquals("setFirstResult() or setMaxResults() specified with collection fetch join (in-memory pagination "
          + "was about to be applied, but 'hibernate.query.fail_on_pagination_over_collection_fetch' is enabled)",
          e.getMessage());
    }
  }

  @Test
  void twoQueryFix() {
    emf.runInTransaction(em -> {
      List<Company> companies = JobBoard.findCompaniesWithPostingsTwoQueries(em, 1, 2);
      assertEquals(List.of("Acme", "Bluebird"), titles(companies));
      assertEquals(4, companies.getFirst().getPostings().size());
    });
    assertEquals(2, sql.statements().size());
    assertEquals("select c1_0.id from Company c1_0 order by c1_0.name offset ? rows fetch first ? rows only",
        sql.statements().get(0));
    assertTrue(sql.statements().get(1).endsWith("where c1_0.id in (?,?) order by c1_0.name"));
  }

  @Test
  void scrollAndStreamReadEveryRowInOneQuery() {
    emf.runInTransaction(em -> {
      assertEquals(new BigDecimal("955000.00"), JobBoard.totalSalaryWithScroll(em));
      assertEquals(new BigDecimal("955000.00"), JobBoard.totalSalaryWithStream(em));
    });
    assertEquals(2, sql.statements().size());
    sql.statements().forEach(s -> {
      assertTrue(s.endsWith("from JobPosting jp1_0 order by jp1_0.id"));
      assertFalse(s.contains("fetch first"));
    });
  }

  @Test
  void hqlLimitOffsetAndNativeQuery() {
    List<String> expected = List.of("Scrum Master", "Backend Developer", "Mobile Developer", "Support Engineer",
        "Product Manager");
    emf.runInTransaction(em -> {
      assertEquals(expected, titles(JobBoard.findWithHqlLimit(em)));
      assertTrue(sql.last().endsWith("order by jp1_0.id offset 5 rows fetch first 5 rows only"));
      assertEquals(expected, JobBoard.findTitlesWithNativeQuery(em, 2, 5));
      assertEquals("select title from JobPosting order by id offset ? rows fetch next ? rows only", sql.last());
    });
  }
}
