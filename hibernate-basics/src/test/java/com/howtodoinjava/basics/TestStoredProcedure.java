package com.howtodoinjava.basics;

import com.howtodoinjava.basics.entity.EmployeeEntity;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.procedure.ProcedureParameter;
import org.hibernate.result.Output;
import org.hibernate.result.UpdateCountOutput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.LongStream;

public class TestStoredProcedure {
  private static SessionFactory sessionFactory = null;

  @BeforeAll
  static void setup() {
    try {
      StandardServiceRegistry standardRegistry
          = new StandardServiceRegistryBuilder()
          .configure("hibernate-mysql.cfg.xml")
          .build();

      Metadata metadata = new MetadataSources(standardRegistry)
          .addAnnotatedClass(EmployeeEntity.class)
          .getMetadataBuilder()
          .build();

      sessionFactory = metadata
          .getSessionFactoryBuilder()
          .build();

      setupdata();

    } catch (Throwable ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  private static void setupdata() {
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
  public void fetchEntityRows() {
    try (Session session = sessionFactory.openSession()) {
      session.getTransaction().begin();

      ProcedureCall call = session.createStoredProcedureQuery(
          "get_employee_details_by_id");

      ProcedureParameter<Long> parameter = call.registerParameter(1,
          Long.class, ParameterMode.IN);
      call.setParameter(parameter, 1L);

      List<Object[]> listOfResults = call.getResultList();
      Object[] resultArray = listOfResults.get(0);

      Assertions.assertEquals(1, resultArray[0]);
      Assertions.assertEquals("NAME_1@email.com",
          resultArray[1]);
      Assertions.assertEquals("FNAME_1",
          resultArray[2]);
      Assertions.assertEquals("LNAME_1",
          resultArray[3]);

      session.getTransaction().commit();
    }
  }

  @Test
  public void mapOutputParameters() {
    try (Session session = sessionFactory.openSession()) {
      session.getTransaction().begin();

      ProcedureCall call = session.createStoredProcedureCall(
          "get_employee_by_id");
      ProcedureParameter<Long> parameter1 = call.registerParameter(1,
          Long.class, ParameterMode.IN);
      ProcedureParameter<String> parameter2 = call.registerParameter(2,
          String.class, ParameterMode.OUT);
      ProcedureParameter<String> parameter3 = call.registerParameter(3,
          String.class, ParameterMode.OUT);
      ProcedureParameter<String> parameter4 = call.registerParameter(4,
          String.class, ParameterMode.OUT);

      call.setParameter(parameter1, 1L);

      Output output = call.getOutputs().getCurrent();

      int updateCount =
          ((UpdateCountOutput) output).getUpdateCount();

      Assertions.assertEquals("NAME_1@email.com",
          call.getOutputs().getOutputParameterValue(2));
      Assertions.assertEquals("FNAME_1",
          call.getOutputs().getOutputParameterValue(3));
      Assertions.assertEquals("LNAME_1",
          call.getOutputs().getOutputParameterValue(4));
      Assertions.assertEquals(1, updateCount);

      session.getTransaction().commit();
    }
  }

  @Test
  public void testNamedStoredProcedure() {
    try (Session session = sessionFactory.openSession()) {
      session.getTransaction().begin();

      StoredProcedureQuery call
          = session.createNamedStoredProcedureQuery("getEmployeeByIdProcedure");

      List<EmployeeEntity> list =
          call.setParameter("employeeId", 1)
              .getResultList();

      EmployeeEntity employee = list.get(0);

      Assertions.assertEquals(1, employee.getEmployeeId());
      Assertions.assertEquals("NAME_1@email.com",
          employee.getEmail());
      Assertions.assertEquals("FNAME_1",
          employee.getFirstName());
      Assertions.assertEquals("LNAME_1",
          employee.getLastName());

      session.getTransaction().commit();
    }
  }
}
