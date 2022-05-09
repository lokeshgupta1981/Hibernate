package com.howtodoinjava.basics;

import com.howtodoinjava.basics.entity.EmployeeEntity;
import jakarta.persistence.TypedQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.query.Query;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

public class TestPagination {
  private static SessionFactory sessionFactory = null;

  @BeforeAll
  static void setup() {
    try {
      StandardServiceRegistry standardRegistry
          = new StandardServiceRegistryBuilder()
          .configure("hibernate-test.cfg.xml")
          .build();

      Metadata metadata = new MetadataSources(standardRegistry)
          .addAnnotatedClass(EmployeeEntity.class)
          .getMetadataBuilder()
          .build();

      sessionFactory = metadata
          .getSessionFactoryBuilder().build();

      setupData();

    } catch (Throwable ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  private static void setupData() {
    try (Session session = sessionFactory.openSession()) {
      session.getTransaction().begin();

      LongStream.range(1, 55).forEach(id -> {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setFirstName("FNAME_" + id);
        employee.setLastName("LNAME_" + id);
        employee.setEmail("NAME_" + id + "@email.com");

        session.persist(employee);
      });

      session.getTransaction().commit();
    }
  }

  @Test
  public void testPaginationUsingHql() {

    PaginationResult<EmployeeEntity> firstPage = paginateUsingHql(1, 10);

    //@formatter:off
    Assertions.assertEquals(54, firstPage.getTotalRecords());
    Assertions.assertEquals(6, firstPage.getLastPageNumber());
    Assertions.assertEquals(10, firstPage.getRecords().size());
    Assertions.assertEquals(1, firstPage.getRecords().get(0).getEmployeeId());
    Assertions.assertEquals(10, firstPage.getRecords().get(9).getEmployeeId());

    PaginationResult<EmployeeEntity> fourthPage = paginateUsingHql(4, 10);

    //@formatter:off
    Assertions.assertEquals(54, fourthPage.getTotalRecords());
    Assertions.assertEquals(6, fourthPage.getLastPageNumber());
    Assertions.assertEquals(10, fourthPage.getRecords().size());
    Assertions.assertEquals(31, fourthPage.getRecords().get(0).getEmployeeId());
    Assertions.assertEquals(40, fourthPage.getRecords().get(9).getEmployeeId());
    //@formatter:on

    PaginationResult<EmployeeEntity> lastPage = paginateUsingHql(6, 10);

    //@formatter:off
    Assertions.assertEquals(54, lastPage.getTotalRecords());
    Assertions.assertEquals(6, lastPage.getLastPageNumber());
    Assertions.assertEquals(4, lastPage.getRecords().size());
    Assertions.assertEquals(51, lastPage.getRecords().get(0).getEmployeeId());
    Assertions.assertEquals(54, lastPage.getRecords().get(3).getEmployeeId());
    //@formatter:on
  }

  private PaginationResult<EmployeeEntity> paginateUsingHql(int pageNumber,
                                                            int pageSize) {
    int lastPageNumber;
    Long totalRecords;
    List<EmployeeEntity> employeeList;

    try (Session session = sessionFactory.openSession()) {
      session.getTransaction().begin();

      TypedQuery countQuery = session.createQuery("SELECT COUNT(e.id) from " +
          "EmployeeEntity e", Long.class);
      totalRecords = (Long) countQuery.getSingleResult();

      if (totalRecords % pageSize == 0) {
        lastPageNumber = (int) (totalRecords / pageSize);
      } else {
        lastPageNumber = (int) (totalRecords / pageSize) + 1;
      }

      session.getTransaction().commit();
    }


    try (Session session = sessionFactory.openSession()) {
      session.getTransaction().begin();

      TypedQuery<EmployeeEntity> query = session.createQuery("From " +
              "EmployeeEntity e ORDER BY e.id",
          EmployeeEntity.class);

      query.setFirstResult((pageNumber - 1) * pageSize);
      query.setMaxResults(pageSize);

      employeeList = query.getResultList();

      session.getTransaction().commit();
    }

    PaginationResult<EmployeeEntity> result = new PaginationResult<>();
    result.setCurrentPageNumber(pageNumber);
    result.setPageSize(pageSize);
    result.setLastPageNumber(lastPageNumber);
    result.setTotalRecords(totalRecords);
    result.setRecords(employeeList);

    return result;
  }

  @Test
  public void testPaginationUsingScrollableResults() {

    PaginationResult<EmployeeEntity> firstPage =
        paginateUsingScrollableResults(1, 10);

    //@formatter:off
    Assertions.assertEquals(54, firstPage.getTotalRecords());
    Assertions.assertEquals(6, firstPage.getLastPageNumber());
    Assertions.assertEquals(10, firstPage.getRecords().size());
    Assertions.assertEquals(1, firstPage.getRecords().get(0).getEmployeeId());
    Assertions.assertEquals(10, firstPage.getRecords().get(9).getEmployeeId());

    PaginationResult<EmployeeEntity> fourthPage = paginateUsingScrollableResults(4, 10);

    //@formatter:off
    Assertions.assertEquals(54, fourthPage.getTotalRecords());
    Assertions.assertEquals(6, fourthPage.getLastPageNumber());
    Assertions.assertEquals(10, fourthPage.getRecords().size());
    Assertions.assertEquals(31, fourthPage.getRecords().get(0).getEmployeeId());
    Assertions.assertEquals(40, fourthPage.getRecords().get(9).getEmployeeId());
    //@formatter:on

    PaginationResult<EmployeeEntity> lastPage =
        paginateUsingScrollableResults(6, 10);

    //@formatter:off
    Assertions.assertEquals(54, lastPage.getTotalRecords());
    Assertions.assertEquals(6, lastPage.getLastPageNumber());
    Assertions.assertEquals(4, lastPage.getRecords().size());
    Assertions.assertEquals(51, lastPage.getRecords().get(0).getEmployeeId());
    Assertions.assertEquals(54, lastPage.getRecords().get(3).getEmployeeId());
    //@formatter:on
  }

  private PaginationResult<EmployeeEntity> paginateUsingScrollableResults(int pageNumber,
                                                                          int pageSize) {
    int lastPageNumber = 0;
    int totalRecords = 0;
    List<EmployeeEntity> employeeList = new ArrayList<>();


    try (Session session = sessionFactory.openSession()) {
      session.getTransaction().begin();

      Query<EmployeeEntity> query = session.createQuery("From " +
              "EmployeeEntity e ORDER BY e.id",
          EmployeeEntity.class);

      try (ScrollableResults resultScroll =
               query.scroll(ScrollMode.SCROLL_INSENSITIVE)) {
        boolean hasRecords = resultScroll.first();
        if (hasRecords) {

          int fromRecordIndex = (pageNumber - 1) * pageSize;
          int maxRecordIndex = (fromRecordIndex + pageSize) - 1;

          hasRecords = resultScroll.scroll(fromRecordIndex);

          if (hasRecords) {
            do {
              EmployeeEntity employee = (EmployeeEntity) resultScroll.get();
              employeeList.add(employee);
            } while (resultScroll.next()
                && resultScroll.getRowNumber() >= fromRecordIndex
                && resultScroll.getRowNumber() <= maxRecordIndex);
          }

          // Go to Last record.
          resultScroll.last();
          totalRecords = resultScroll.getRowNumber() + 1;
          if (totalRecords % pageSize == 0) {
            lastPageNumber = (int) (totalRecords / pageSize);
          } else {
            lastPageNumber = (int) (totalRecords / pageSize) + 1;
          }
        }
      }

      session.getTransaction().commit();
    }

    PaginationResult<EmployeeEntity> result = new PaginationResult<>();
    result.setCurrentPageNumber(pageNumber);
    result.setPageSize(pageSize);
    result.setLastPageNumber(lastPageNumber);
    result.setTotalRecords(totalRecords);
    result.setRecords(employeeList);

    return result;
  }
}


class PaginationResult<E> {

  int currentPageNumber;
  int lastPageNumber;
  int pageSize;
  long totalRecords;
  List<E> records;

  public int getCurrentPageNumber() {
    return currentPageNumber;
  }

  public void setCurrentPageNumber(int currentPageNumber) {
    this.currentPageNumber = currentPageNumber;
  }

  public int getLastPageNumber() {
    return lastPageNumber;
  }

  public void setLastPageNumber(int lastPageNumber) {
    this.lastPageNumber = lastPageNumber;
  }

  public int getPageSize() {
    return pageSize;
  }

  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }

  public long getTotalRecords() {
    return totalRecords;
  }

  public void setTotalRecords(long totalRecords) {
    this.totalRecords = totalRecords;
  }

  public List<E> getRecords() {
    return records;
  }

  public void setRecords(List<E> records) {
    this.records = records;
  }

  @Override
  public String toString() {
    return "PaginationResult{" +
        "currentPageNumber=" + currentPageNumber +
        ", lastPageNumber=" + lastPageNumber +
        ", pageSize=" + pageSize +
        ", totalRecords=" + totalRecords +
        '}';
  }
}
