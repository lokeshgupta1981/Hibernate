package com.howtodoinjava.hibernate.notfound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.RollbackException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.hibernate.FetchNotFoundException;
import org.hibernate.Hibernate;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.cfg.Configuration;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class NotFoundTest {

  private static final String NO_ROW_99 = "No row with the given identifier exists for entity "
      + "[com.howtodoinjava.hibernate.notfound.Scholarship with id '99']";

  private static final String ORPHANS = NotFoundDemo.ORPHANS;

  private EntityManagerFactory emf;
  private Long meritId;

  private void start(Class<? extends BaseApplication> applicationClass) {
    emf = Database.create(applicationClass, false);
    meritId = Database.insertLegacyData(emf);
    Database.statements(emf);
  }

  @AfterEach
  void tearDown() {
    if (emf != null) {
      emf.close();
    }
  }

  // ---- 1. Causes -------------------------------------------------------------------------------

  @Test
  void getReferenceOfMissingIdFailsOnFirstGetter() {
    start(ScholarshipApplication.class);
    EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
        () -> emf.runInTransaction(em -> {
          Scholarship ref = em.getReference(Scholarship.class, 99L);
          assertEquals("Scholarship$HibernateProxy", ref.getClass().getSimpleName());
          assertEquals(99L, ref.getId());
          assertEquals(0, Database.statements(emf));
          ref.getName();
        }));
    assertEquals(NO_ROW_99, e.getMessage());
    assertInstanceOf(ObjectNotFoundException.class, e.getCause());
    assertEquals(NO_ROW_99, e.getCause().getMessage());
  }

  @Test
  void hibernateInitializeOfMissingIdFails() {
    start(ScholarshipApplication.class);
    assertThrows(EntityNotFoundException.class, () -> emf.runInTransaction(em ->
        Hibernate.initialize(em.getReference(Scholarship.class, 99L))));
  }

  @Test
  void nativeSessionFactoryThrowsObjectNotFoundException() {
    try (SessionFactory sf = new Configuration()
        .addAnnotatedClass(Scholarship.class)
        .setProperty("hibernate.connection.url", "jdbc:h2:mem:native-test;DB_CLOSE_DELAY=-1")
        .setProperty("hibernate.connection.username", "sa")
        .setProperty("hibernate.hbm2ddl.auto", "create-drop")
        .buildSessionFactory()) {
      ObjectNotFoundException e = assertThrows(ObjectNotFoundException.class, () ->
          sf.inTransaction(session -> session.getReference(Scholarship.class, 99L).getName()));
      assertEquals(NO_ROW_99, e.getMessage());
      assertFalse(EntityNotFoundException.class.isInstance(e));
      assertNull(e.getCause());
    }
  }

  @Test
  void lazyAssociationToMissingRowFailsOnAccess() {
    start(ScholarshipApplication.class);
    EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
        () -> emf.runInTransaction(em -> {
          ScholarshipApplication app = em.find(ScholarshipApplication.class, 1L);
          assertEquals("Lokesh", app.getStudentName());
          assertFalse(Hibernate.isInitialized(app.getScholarship()));
          assertEquals(99L, app.getScholarship().getId());
          assertEquals(1, Database.statements(emf));
          app.getScholarship().getName();
        }));
    assertEquals(NO_ROW_99, e.getMessage());
  }

  @Test
  void eagerAssociationToMissingRowFailsInFind() {
    start(EagerApplication.class);
    FetchNotFoundException e = assertThrows(FetchNotFoundException.class,
        () -> emf.runInTransaction(em -> em.find(EagerApplication.class, 1L)));
    assertEquals("Entity `com.howtodoinjava.hibernate.notfound.Scholarship` with identifier value "
        + "`99` does not exist", e.getMessage());
    assertInstanceOf(EntityNotFoundException.class, e);
    assertNull(e.getCause());
  }

  @Test
  void eagerAssociationToMissingRowFailsInQuery() {
    start(EagerApplication.class);
    EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
        () -> emf.runInTransaction(em -> em
            .createQuery("from ScholarshipApplication", EagerApplication.class).getResultList()));
    assertEquals(NO_ROW_99, e.getMessage());
    assertFalse(e instanceof FetchNotFoundException);
  }

  @Test
  void rowDeletedByAnotherTransactionAfterProxyWasCreated() {
    start(ScholarshipApplication.class);
    EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
        () -> emf.runInTransaction(em -> {
          Scholarship merit = em.find(ScholarshipApplication.class, 2L).getScholarship();
          emf.runInTransaction(other -> other.createQuery("delete from Scholarship where id = :id")
              .setParameter("id", meritId).executeUpdate());
          merit.getName();
        }));
    assertEquals("No row with the given identifier exists for entity "
        + "[com.howtodoinjava.hibernate.notfound.Scholarship with id '" + meritId + "']",
        e.getMessage());
  }

  @Test
  void refreshOfDeletedRowFails() {
    start(ScholarshipApplication.class);
    EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
        () -> emf.runInTransaction(em -> {
          Scholarship merit = em.find(Scholarship.class, meritId);
          emf.runInTransaction(other -> other.createQuery("delete from Scholarship")
              .executeUpdate());
          em.refresh(merit);
        }));
    assertEquals("No row with the given identifier exists for entity "
        + "[com.howtodoinjava.hibernate.notfound.Scholarship with id '" + meritId + "']",
        e.getMessage());
    assertInstanceOf(UnresolvableObjectException.class, e.getCause());
    assertFalse(e.getCause() instanceof ObjectNotFoundException);
  }

  // ---- 2. Fixes --------------------------------------------------------------------------------

  @Test
  void findOfMissingIdReturnsNull() {
    start(ScholarshipApplication.class);
    assertNull(emf.callInTransaction(em -> em.find(Scholarship.class, 99L)));
    assertEquals("Merit", emf.callInTransaction(em -> em.find(Scholarship.class, meritId)).getName());
  }

  @Test
  void findByProxyIdChecksTheAssociationWithoutException() {
    start(ScholarshipApplication.class);
    emf.runInTransaction(em -> {
      ScholarshipApplication lokesh = em.find(ScholarshipApplication.class, 1L);
      assertNull(em.find(Scholarship.class, lokesh.getScholarship().getId()));
      ScholarshipApplication alex = em.find(ScholarshipApplication.class, 2L);
      assertEquals("Merit", em.find(Scholarship.class, alex.getScholarship().getId()).getName());
    });
  }

  @Test
  void orphanQueryFindsAndDeletesTheBrokenApplication() {
    start(ScholarshipApplication.class);
    assertEquals(List.of("Lokesh"), emf.callInTransaction(em -> em.createQuery(
        "select a.studentName from ScholarshipApplication a " + ORPHANS, String.class)
        .getResultList()));
    assertEquals(Integer.valueOf(1), emf.<Integer>callInTransaction(em -> em.createQuery(
        "delete from ScholarshipApplication a " + ORPHANS).executeUpdate()));
    assertEquals(List.of("Alex"), emf.callInTransaction(em -> em.createQuery(
        "select a.studentName from ScholarshipApplication a", String.class).getResultList()));
  }

  @Test
  void orphanUpdateSetsForeignKeyToNull() {
    start(ScholarshipApplication.class);
    assertEquals(Integer.valueOf(1), emf.<Integer>callInTransaction(em -> em.createQuery(
        "update ScholarshipApplication a set a.scholarship = null " + ORPHANS).executeUpdate()));
    assertNull(scholarshipIdOfApplication1());
  }

  @Test
  void foreignKeyCanBeAddedOnlyAfterCleanup() {
    start(ScholarshipApplication.class);
    String addForeignKey = """
        alter table scholarship_application
          add constraint fk_application_scholarship
          foreign key (scholarship_id) references Scholarship (id)""";
    ConstraintViolationException e = assertThrows(ConstraintViolationException.class,
        () -> emf.runInTransaction(em -> em.createNativeQuery(addForeignKey).executeUpdate()));
    assertTrue(e.getMessage().contains("Referential integrity constraint violation: "
        + "\"FK_APPLICATION_SCHOLARSHIP: PUBLIC.SCHOLARSHIP_APPLICATION FOREIGN KEY(SCHOLARSHIP_ID) "
        + "REFERENCES PUBLIC.SCHOLARSHIP(ID)\""));

    emf.runInTransaction(em -> em.createQuery(
        "delete from ScholarshipApplication a " + ORPHANS).executeUpdate());
    emf.runInTransaction(em -> em.createNativeQuery(addForeignKey).executeUpdate());

    // the database now rejects new broken rows and deleting a scholarship in use
    assertThrows(ConstraintViolationException.class, () -> Database.insertLegacyData(emf));
    RollbackException rollback = assertThrows(RollbackException.class,
        () -> emf.runInTransaction(em -> em.remove(em.find(Scholarship.class, meritId))));
    assertInstanceOf(ConstraintViolationException.class, rollback.getCause());
  }

  @Test
  void mappingWithForeignKeyRejectsMissingScholarship() {
    emf = Database.create(CheckedApplication.class, false);
    ConstraintViolationException e = assertThrows(ConstraintViolationException.class,
        () -> Database.insertLegacyData(emf));
    assertTrue(e.getMessage().contains("Referential integrity constraint violation"));
    assertTrue(e.getMessage().contains("(CAST(99 AS BIGINT))"));
    assertThrows(ConstraintViolationException.class, () -> emf.runInTransaction(em ->
        em.persist(new CheckedApplication("Lokesh", LocalDate.of(2026, 9, 15),
            em.getReference(Scholarship.class, 99L)))));
    // the constraint does not change getReference() with an id that has no row
    EntityNotFoundException e2 = assertThrows(EntityNotFoundException.class, () ->
        emf.runInTransaction(em -> em.getReference(Scholarship.class, 99L).getName()));
    assertEquals(NO_ROW_99, e2.getMessage());
  }

  @Test
  void notFoundIgnoreTurnsMissingRowIntoNull() {
    start(IgnoreApplication.class);
    emf.runInTransaction(em -> {
      IgnoreApplication lokesh = em.find(IgnoreApplication.class, 1L);
      assertNull(lokesh.getScholarship());
    });
  }

  @Test
  void notFoundIgnoreForcesEagerLoading() {
    start(IgnoreApplication.class);
    emf.runInTransaction(em -> {
      IgnoreApplication alex = em.find(IgnoreApplication.class, 2L);
      assertEquals(2, Database.statements(emf));                  // application + scholarship
      assertTrue(Hibernate.isInitialized(alex.getScholarship()));
      assertEquals(Scholarship.class, alex.getScholarship().getClass());  // no proxy
    });
    // the LAZY mapping without @NotFound runs one select for the same call
    try (EntityManagerFactory lazy = Database.create(ScholarshipApplication.class, false)) {
      Database.insertLegacyData(lazy);
      Database.statements(lazy);
      lazy.runInTransaction(em -> em.find(ScholarshipApplication.class, 2L));
      assertEquals(1, Database.statements(lazy));
    }
  }

  @Test
  void notFoundIgnoreRunsOneExtraSelectPerApplicationInQueries() {
    start(IgnoreApplication.class);
    List<IgnoreApplication> apps = emf.callInTransaction(em -> em
        .createQuery("from ScholarshipApplication order by id", IgnoreApplication.class)
        .getResultList());
    assertEquals(3, Database.statements(emf));                     // 1 + 2 scholarship selects
    assertNull(apps.get(0).getScholarship());
    assertEquals("Merit", apps.get(1).getScholarship().getName());
  }

  @Test
  void notFoundIgnoreMatchesBrokenRowInIsNullQuery() {
    start(IgnoreApplication.class);
    assertEquals(List.of("Lokesh"), emf.callInTransaction(em -> em.createQuery(
        "select a.studentName from ScholarshipApplication a where a.scholarship is null",
        String.class).getResultList()));
  }

  @Test
  void notFoundIgnoreErasesForeignKeyWhenApplicationIsSaved() {
    start(IgnoreApplication.class);
    assertEquals(99L, scholarshipIdOfApplication1());
    emf.runInTransaction(em -> em.find(IgnoreApplication.class, 1L).setStudentName("Lokesh Gupta"));
    assertNull(scholarshipIdOfApplication1());
  }

  @Test
  void notFoundMappingsCreateNoForeignKey() {
    start(IgnoreApplication.class);
    // without a constraint the database accepts deleting a scholarship that is in use
    emf.runInTransaction(em -> em.remove(em.find(Scholarship.class, meritId)));
    assertNull(emf.callInTransaction(em -> em.find(IgnoreApplication.class, 2L)).getScholarship());
  }

  @Test
  void notFoundExceptionFailsWhileLoadingTheApplication() {
    start(ExceptionApplication.class);
    FetchNotFoundException e = assertThrows(FetchNotFoundException.class,
        () -> emf.runInTransaction(em -> em.find(ExceptionApplication.class, 1L)));
    assertEquals("Entity `com.howtodoinjava.hibernate.notfound.Scholarship` with identifier value "
        + "`99` does not exist", e.getMessage());
  }

  // ---- 3. FAQs ---------------------------------------------------------------------------------

  @Test
  void exceptionHierarchy() {
    assertTrue(jakarta.persistence.PersistenceException.class
        .isAssignableFrom(ObjectNotFoundException.class));
    assertFalse(EntityNotFoundException.class.isAssignableFrom(ObjectNotFoundException.class));
    assertEquals(UnresolvableObjectException.class, ObjectNotFoundException.class.getSuperclass());
    assertEquals(EntityNotFoundException.class, FetchNotFoundException.class.getSuperclass());
  }

  @Test
  void sessionLoadByClassIsGoneAndGetIsDeprecated() throws NoSuchMethodException {
    List<Method> loads = Arrays.stream(Session.class.getMethods())
        .filter(m -> m.getName().equals("load")).toList();
    assertEquals(1, loads.size());                                  // only load(Object, Object)
    assertEquals(Object.class, loads.get(0).getParameterTypes()[0]);
    assertTrue(Session.class.getMethod("get", Class.class, Object.class)
        .isAnnotationPresent(Deprecated.class));
  }

  private Object scholarshipIdOfApplication1() {
    return emf.callInTransaction(em -> em.createNativeQuery(
        "select scholarship_id from scholarship_application where id = 1").getSingleResult());
  }
}
