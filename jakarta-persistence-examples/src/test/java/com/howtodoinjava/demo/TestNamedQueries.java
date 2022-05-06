package com.howtodoinjava.demo;

import com.howtodoinjava.demo.entity.DepartmentEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.Query;
import org.junit.jupiter.api.*;

public class TestNamedQueries {

  private static EntityManagerFactory emf;
  private static EntityManager em;

  @BeforeAll
  static void setup() {
    emf = Persistence.createEntityManagerFactory("H2DB");
    em = emf.createEntityManager();
    em.getTransaction().begin();
  }

  @AfterAll
  static void tear() {
    em.close();
    emf.close();
  }

  @BeforeEach
  void setupThis() {
    em = emf.createEntityManager();
    em.getTransaction().begin();

    DepartmentEntity d1 = new DepartmentEntity();
    d1.setName("HR");

    DepartmentEntity d2 = new DepartmentEntity();
    d2.setName("Finance");

    em.persist(d1);
    em.persist(d2);
    flushAndClear();
  }

  @AfterEach
  void tearThis() {
    em.getTransaction().commit();
    em.close();
  }

  @Test
  public void getEntityById() {
    Query query =
        em.createNamedQuery(DepartmentEntity.QUERY_GET_DEPARTMENT_BY_ID)
            .setParameter("id", 1);

    DepartmentEntity dept = (DepartmentEntity) query.getSingleResult();

    Assertions.assertEquals("HR", dept.getName());
  }

  //@Test
  public void updateEntityById() {
    Query query =
        em.createNamedQuery(DepartmentEntity.QUERY_UPDATE_DEPARTMENT_BY_ID)
            .setParameter("name", "TRANSPORT")
            .setParameter("id", 1);

    query.getFirstResult();
    flushAndClear();

    DepartmentEntity dept = em.find(DepartmentEntity.class, 1);

    Assertions.assertEquals("TRANSPORT", dept.getName());
  }

  @Test
  public void updateEntityByIdWithNamedNativeQuery() {
    Query query =
        em.createNamedQuery(DepartmentEntity.NATIVE_QUERY_UPDATE_DEPARTMENT_BY_ID)
            .setParameter("name", "SUPPORT")
            .setParameter("id", 1);

    query.executeUpdate();
    flushAndClear();

    DepartmentEntity dept = em.find(DepartmentEntity.class, 1);

    Assertions.assertEquals("SUPPORT", dept.getName());
  }

  private void flushAndClear() {
    em.flush();
    em.clear();
  }
}
