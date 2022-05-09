package com.howtodoinjava.demo.mappings.sorting;

import com.howtodoinjava.hibernate.onetoone.dto.foreignKeyAsso.AccountEntity;
import com.howtodoinjava.hibernate.onetoone.dto.foreignKeyAsso.EmployeeEntity;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.query.SelectionQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class SimpleSorting {
  private static SessionFactory sessionFactory = null;

  @BeforeAll
  static void setup() {
    try {
      StandardServiceRegistry standardRegistry
          = new StandardServiceRegistryBuilder()
          .configure("hibernate-test.cfg.xml")
          .build();

      Metadata metadata = new MetadataSources(standardRegistry)
          .addAnnotatedClass(AccountEntity.class)
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

  public static void setupData() {
    try (Session session = sessionFactory.openSession()) {
      session.getTransaction().begin();
      EmployeeEntity e1 = EmployeeEntity.builder()
          .firstName("Lokesh")
          .lastName("Gupta")
          .email("gamma@email.com")
          .build();

      EmployeeEntity e2 = EmployeeEntity.builder()
          .firstName("Akash")
          .lastName("Gupta")
          .email("beta@email.com")
          .build();

      EmployeeEntity e3 = EmployeeEntity.builder()
          .firstName("Vivek")
          .lastName("Gupta")
          .email("alpha@email.com")
          .build();

      session.persist(e1);
      session.persist(e2);
      session.persist(e3);

      session.getTransaction().commit();
    }
  }

  @Test
  public void sortUsingHQL() {

    try (Session session = sessionFactory.openSession()) {
      session.getTransaction().begin();

      //1 HQL
      String hql = "FROM ForeignKeyAssEmployeeEntity e ORDER BY e.firstName " +
          "ASC, e.email DESC NULLS FIRST";
      SelectionQuery query = session.createQuery(hql, EmployeeEntity.class);
      List<EmployeeEntity> list = query.getResultList();

      Assertions.assertEquals(2, list.get(0).getEmployeeId());
      Assertions.assertEquals(1, list.get(1).getEmployeeId());
      Assertions.assertEquals(3, list.get(2).getEmployeeId());

      session.getTransaction().commit();
    }
  }

  @Test
  public void sortUsingCriteriaAPI() {

    try (Session session = sessionFactory.openSession()) {
      session.getTransaction().begin();
      CriteriaBuilder builder = session.getCriteriaBuilder();
      CriteriaQuery<EmployeeEntity> criteriaQuery =
          builder.createQuery(EmployeeEntity.class);

      Root<EmployeeEntity> root = criteriaQuery.from(EmployeeEntity.class);

      List<Order> orderList = new ArrayList();
      orderList.add(builder.asc(root.get("firstName")));
      orderList.add(builder.desc(root.get("email")));


      TypedQuery<EmployeeEntity> query = session
          .createQuery(criteriaQuery.select(root).orderBy(orderList));

      List<EmployeeEntity> list = query.getResultList();
      session.getTransaction().commit();

      Assertions.assertEquals(2, list.get(0).getEmployeeId());
      Assertions.assertEquals(1, list.get(1).getEmployeeId());
      Assertions.assertEquals(3, list.get(2).getEmployeeId());
    }
  }

  @Test
  public void sortUsingNativeSQLQuery() {

    try (Session session = sessionFactory.openSession()) {
      session.getTransaction().begin();

      List<EmployeeEntity> list = session
          .createNativeQuery(
              "SELECT * FROM Employee e ORDER BY e.FIRST_NAME ASC, e.EMAIL " +
                  "DESC NULLS FIRST",
              EmployeeEntity.class)
          .list();

      session.getTransaction().commit();

      Assertions.assertEquals(2, list.get(0).getEmployeeId());
      Assertions.assertEquals(1, list.get(1).getEmployeeId());
      Assertions.assertEquals(3, list.get(2).getEmployeeId());
    }
  }
}
