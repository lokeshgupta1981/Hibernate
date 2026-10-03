package com.howtodoinjava.hibernate.savechild;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.RollbackException;
import java.util.Arrays;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.TransientObjectException;
import org.hibernate.TransientPropertyValueException;
import org.junit.jupiter.api.Test;

class SaveChildEntitiesTest {

  private static List<String> changes(SqlLog log) {
    return log.statements().stream()
        .filter(s -> s.startsWith("insert") || s.startsWith("update") || s.startsWith("delete"))
        .toList();
  }

  private static Long persistCoffeeSurvey(EntityManagerFactory emf) {
    return emf.callInTransaction(em -> {
      Survey survey = new Survey("Coffee Habits");
      survey.addQuestion(new Question("How many cups a day?", 1));
      survey.addQuestion(new Question("Favorite drink?", 2));
      survey.addQuestion(new Question("Milk or black?", 3));
      em.persist(survey);
      return survey.getId();
    });
  }

  private static Survey loadWithQuestions(EntityManagerFactory emf, Long id) {
    return emf.callInTransaction(em -> em
        .createQuery("select s from Survey s join fetch s.questions where s.id = :id", Survey.class)
        .setParameter("id", id)
        .getSingleResult());
  }

  // ---- cascade PERSIST on a new survey ----

  @Test
  void persistSavesSurveyAndQuestionsParentFirst() {
    SqlLog log = new SqlLog();
    try (EntityManagerFactory emf = Database.create(false, log)) {
      persistCoffeeSurvey(emf);
      assertEquals(List.of(
          "insert into Survey (title,id) values (?,?)",
          "insert into Question (position,survey_id,text,id) values (?,?,?,?)",
          "insert into Question (position,survey_id,text,id) values (?,?,?,?)",
          "insert into Question (position,survey_id,text,id) values (?,?,?,?)"), changes(log));
      assertEquals(1, Database.count(emf, "Survey"));
      assertEquals(3, Database.count(emf, "Question"));
    }
  }

  @Test
  void idsAreAssignedAtPersistButInsertsRunAtCommit() {
    SqlLog log = new SqlLog();
    try (EntityManagerFactory emf = Database.create(false, log)) {
      emf.runInTransaction(em -> {
        Survey survey = new Survey("Coffee Habits");
        survey.addQuestion(new Question("How many cups a day?", 1));
        survey.addQuestion(new Question("Favorite drink?", 2));
        survey.addQuestion(new Question("Milk or black?", 3));
        em.persist(survey);
        assertEquals(1L, survey.getId());
        assertEquals(1L, survey.getQuestions().get(0).getId());
        assertTrue(changes(log).isEmpty());
        assertEquals(List.of(
            "select next value for Survey_SEQ",
            "select next value for Question_SEQ",
            "select next value for Question_SEQ"), log.statements());
      });
      assertEquals(4, changes(log).size());
    }
  }

  @Test
  void helperMethodFillsTheForeignKey() {
    try (EntityManagerFactory emf = Database.create(false, new SqlLog())) {
      Long id = persistCoffeeSurvey(emf);
      List<?> fks = emf.callInTransaction(em -> em
          .createNativeQuery("select distinct survey_id from question").getResultList());
      assertEquals(1, fks.size());
      assertEquals(id, ((Number) fks.get(0)).longValue());
    }
  }

  @Test
  void addingOnlyToTheListLeavesTheForeignKeyNull() {
    try (EntityManagerFactory emf = Database.create(false, new SqlLog())) {
      emf.runInTransaction(em -> {
        Survey survey = new Survey("Weekend Plans");
        survey.getQuestions().add(new Question("Beach or hills?", 1));
        em.persist(survey);
      });
      assertEquals(1, Database.count(emf, "Question"));
      Object fk = emf.callInTransaction(em -> em
          .createNativeQuery("select survey_id from question").getSingleResult());
      assertNull(fk);
    }
  }

  private static Class<?> returnTypeOf(String name) {
    try {
      return EntityManager.class.getMethod(name, Object.class).getReturnType();
    } catch (NoSuchMethodException e) {
      throw new AssertionError(e);
    }
  }

  // ---- managed, detached, existing parent ----

  @Test
  void persistOnManagedSurveyOnlyCascades() {
    SqlLog log = new SqlLog();
    try (EntityManagerFactory emf = Database.create(false, log)) {
      Long id = persistCoffeeSurvey(emf);
      log.clear();
      emf.runInTransaction(em -> {
        Survey survey = em.find(Survey.class, id);
        survey.addQuestion(new Question("Tea or coffee?", 4));
        em.persist(survey);
      });
      assertEquals(List.of("insert into Question (position,survey_id,text,id) values (?,?,?,?)"), changes(log));
    }
  }

  @Test
  void removingQuestionFromListKeepsItsRow() {
    try (EntityManagerFactory emf = Database.create(false, new SqlLog())) {
      Long id = persistCoffeeSurvey(emf);
      emf.runInTransaction(em -> {
        Survey survey = em.find(Survey.class, id);
        survey.removeQuestion(survey.getQuestions().get(0));
      });
      assertEquals(3, Database.count(emf, "Question"));
    }
  }

  @Test
  void newQuestionInManagedSurveyIsInsertedAtCommit() {
    SqlLog log = new SqlLog();
    try (EntityManagerFactory emf = Database.create(false, log)) {
      Long id = persistCoffeeSurvey(emf);
      log.clear();
      emf.runInTransaction(em -> em.find(Survey.class, id).addQuestion(new Question("Tea or coffee?", 4)));
      assertEquals(List.of(
          "select s1_0.id,s1_0.title from Survey s1_0 where s1_0.id=?",
          "insert into Question (position,survey_id,text,id) values (?,?,?,?)"), log.statements());
      assertEquals(4, Database.count(emf, "Question"));
    }
  }

  @Test
  void mergeSavesNewQuestionOfDetachedSurvey() {
    SqlLog log = new SqlLog();
    try (EntityManagerFactory emf = Database.create(false, log)) {
      Long id = persistCoffeeSurvey(emf);
      emf.runInTransaction(em -> em.find(Survey.class, id).addQuestion(new Question("Tea or coffee?", 4)));
      Survey detached = loadWithQuestions(emf, id);
      Question sugar = new Question("Sugar?", 5);
      detached.addQuestion(sugar);
      log.clear();
      Survey managed = emf.callInTransaction(em -> em.merge(detached));
      assertEquals(List.of(
          "select s1_0.id,s1_0.title,q1_0.survey_id,q1_0.id,q1_0.position,q1_0.text from Survey s1_0 "
              + "left join Question q1_0 on s1_0.id=q1_0.survey_id where s1_0.id=?",
          "insert into Question (position,survey_id,text,id) values (?,?,?,?)"), log.statements());
      assertEquals(5, Database.count(emf, "Question"));
      assertNull(sugar.getId());
      assertEquals(5L, managed.getQuestions().get(4).getId());
    }
  }

  @Test
  void persistOnDetachedSurveyFails() {
    try (EntityManagerFactory emf = Database.create(false, new SqlLog())) {
      Survey detached = loadWithQuestions(emf, persistCoffeeSurvey(emf));
      EntityExistsException e = assertThrows(EntityExistsException.class,
          () -> emf.runInTransaction(em -> em.persist(detached)));
      assertEquals("Detached entity passed to persist: com.howtodoinjava.hibernate.savechild.Survey", e.getMessage());
    }
  }

  @Test
  void getReferenceSavesQuestionWithoutLoadingTheSurvey() {
    SqlLog log = new SqlLog();
    try (EntityManagerFactory emf = Database.create(false, log)) {
      Long id = persistCoffeeSurvey(emf);
      log.clear();
      emf.runInTransaction(em -> {
        Question decaf = new Question("Decaf?", 6);
        decaf.setSurvey(em.getReference(Survey.class, id));
        em.persist(decaf);
      });
      assertEquals(List.of("insert into Question (position,survey_id,text,id) values (?,?,?,?)"), log.statements());
      Object fk = emf.callInTransaction(em -> em
          .createNativeQuery("select survey_id from question where text = 'Decaf?'").getSingleResult());
      assertEquals(id, ((Number) fk).longValue());
    }
  }

  // ---- without cascade ----

  private static EntityManagerFactory noCascade(SqlLog log) {
    return Database.create(false, log,
        com.howtodoinjava.hibernate.savechild.nocascade.Survey.class,
        com.howtodoinjava.hibernate.savechild.nocascade.Question.class);
  }

  @Test
  void withoutCascadePersistingSurveyIgnoresQuestions() {
    try (EntityManagerFactory emf = noCascade(new SqlLog())) {
      emf.runInTransaction(em -> {
        var survey = new com.howtodoinjava.hibernate.savechild.nocascade.Survey("Coffee Habits");
        survey.addQuestion(new com.howtodoinjava.hibernate.savechild.nocascade.Question("How many cups a day?", 1));
        em.persist(survey);
      });
      assertEquals(1, Database.count(emf, "Survey"));
      assertEquals(0, Database.count(emf, "Question"));
    }
  }

  @Test
  void withoutCascadePersistingQuestionWithNewSurveyFails() {
    try (EntityManagerFactory emf = noCascade(new SqlLog())) {
      RollbackException e = assertThrows(RollbackException.class, () -> emf.runInTransaction(em -> {
        var survey = new com.howtodoinjava.hibernate.savechild.nocascade.Survey("Coffee Habits");
        var q = new com.howtodoinjava.hibernate.savechild.nocascade.Question("Favorite drink?", 2);
        survey.addQuestion(q);
        em.persist(q);
      }));
      Throwable root = e.getCause().getCause();
      assertInstanceOf(TransientPropertyValueException.class, root);
      assertInstanceOf(TransientObjectException.class, root);
      String p = "com.howtodoinjava.hibernate.savechild.nocascade.";
      assertEquals("Persistent instance of '" + p + "Question' references an unsaved transient instance of '"
          + p + "Survey' (persist the transient instance before flushing) [" + p + "Question.survey -> "
          + p + "Survey]", root.getMessage());
      assertEquals(0, Database.count(emf, "Question"));
    }
  }

  @Test
  void withoutCascadeChildFirstAddsAnUpdate() {
    SqlLog log = new SqlLog();
    try (EntityManagerFactory emf = noCascade(log)) {
      emf.runInTransaction(em -> {
        var survey = new com.howtodoinjava.hibernate.savechild.nocascade.Survey("Tea Habits");
        var q = new com.howtodoinjava.hibernate.savechild.nocascade.Question("Green or black?", 1);
        survey.addQuestion(q);
        em.persist(q);
        em.persist(survey);
      });
      assertEquals(List.of(
          "insert into Question (position,survey_id,text,id) values (?,?,?,?)",
          "insert into Survey (title,id) values (?,?)",
          "update Question set position=?,survey_id=?,text=? where id=?"), changes(log));
    }
  }

  @Test
  void withoutCascadeParentFirstNeedsNoUpdate() {
    SqlLog log = new SqlLog();
    try (EntityManagerFactory emf = noCascade(log)) {
      emf.runInTransaction(em -> {
        var survey = new com.howtodoinjava.hibernate.savechild.nocascade.Survey("Lunch Habits");
        var q = new com.howtodoinjava.hibernate.savechild.nocascade.Question("Home or office?", 1);
        survey.addQuestion(q);
        em.persist(survey);
        em.persist(q);
      });
      assertEquals(List.of(
          "insert into Survey (title,id) values (?,?)",
          "insert into Question (position,survey_id,text,id) values (?,?,?,?)"), changes(log));
    }
  }

  @Test
  void withoutCascadeMergeDropsNewQuestion() {
    try (EntityManagerFactory emf = noCascade(new SqlLog())) {
      Long id = emf.callInTransaction(em -> {
        var survey = new com.howtodoinjava.hibernate.savechild.nocascade.Survey("Lunch Habits");
        var q = new com.howtodoinjava.hibernate.savechild.nocascade.Question("Home or office?", 1);
        survey.addQuestion(q);
        em.persist(survey);
        em.persist(q);
        return survey.getId();
      });
      var detached = emf.callInTransaction(em -> em
          .createQuery("select s from Survey s join fetch s.questions where s.id = :id",
              com.howtodoinjava.hibernate.savechild.nocascade.Survey.class)
          .setParameter("id", id).getSingleResult());
      detached.addQuestion(new com.howtodoinjava.hibernate.savechild.nocascade.Question("Dessert?", 2));
      emf.runInTransaction(em -> em.merge(detached));
      assertEquals(1, Database.count(emf, "Question"));
    }
  }

  // ---- unidirectional and IDENTITY variants ----

  @Test
  void unidirectionalOneToManyRunsAnUpdatePerQuestion() {
    SqlLog log = new SqlLog();
    try (EntityManagerFactory emf = Database.create(false, log,
        com.howtodoinjava.hibernate.savechild.unidirectional.Survey.class,
        com.howtodoinjava.hibernate.savechild.unidirectional.Question.class)) {
      emf.runInTransaction(em -> {
        var survey = new com.howtodoinjava.hibernate.savechild.unidirectional.Survey("Coffee Habits");
        survey.getQuestions().add(new com.howtodoinjava.hibernate.savechild.unidirectional.Question("How many cups a day?", 1));
        survey.getQuestions().add(new com.howtodoinjava.hibernate.savechild.unidirectional.Question("Favorite drink?", 2));
        em.persist(survey);
      });
      assertEquals(List.of(
          "insert into Survey (title,id) values (?,?)",
          "insert into Question (position,text,id) values (?,?,?)",
          "insert into Question (position,text,id) values (?,?,?)",
          "update Question set survey_id=? where id=?",
          "update Question set survey_id=? where id=?"), changes(log));
    }
  }

  @Test
  void identityInsertsDuringPersistAndAllCascadesPersist() {
    SqlLog log = new SqlLog();
    try (EntityManagerFactory emf = Database.create(false, log,
        com.howtodoinjava.hibernate.savechild.identity.Survey.class,
        com.howtodoinjava.hibernate.savechild.identity.Question.class)) {
      emf.runInTransaction(em -> {
        var survey = new com.howtodoinjava.hibernate.savechild.identity.Survey("Coffee Habits");
        survey.addQuestion(new com.howtodoinjava.hibernate.savechild.identity.Question("How many cups a day?", 1));
        survey.addQuestion(new com.howtodoinjava.hibernate.savechild.identity.Question("Favorite drink?", 2));
        log.clear();
        em.persist(survey);
        assertEquals(List.of(
            "insert into Survey (title,id) values (?,default)",
            "insert into Question (position,survey_id,text,id) values (?,?,?,default)",
            "insert into Question (position,survey_id,text,id) values (?,?,?,default)"), changes(log));
        assertEquals(1L, survey.getId());
      });
      assertEquals(2, Database.count(emf, "Question"));

      // CascadeType.ALL includes REMOVE: deleting the survey deletes its questions
      emf.runInTransaction(em -> em.remove(em.find(com.howtodoinjava.hibernate.savechild.identity.Survey.class, 1L)));
      assertEquals(0, Database.count(emf, "Question"));
      assertEquals(0, Database.count(emf, "Survey"));
    }
  }

  @Test
  void removingQuestionFromListKeepsItsRowWithCascadeAll() {
    try (EntityManagerFactory emf = Database.create(false, new SqlLog(),
        com.howtodoinjava.hibernate.savechild.identity.Survey.class,
        com.howtodoinjava.hibernate.savechild.identity.Question.class)) {
      emf.runInTransaction(em -> {
        var survey = new com.howtodoinjava.hibernate.savechild.identity.Survey("Coffee Habits");
        survey.addQuestion(new com.howtodoinjava.hibernate.savechild.identity.Question("How many cups a day?", 1));
        em.persist(survey);
      });
      emf.runInTransaction(em -> {
        var survey = em.find(com.howtodoinjava.hibernate.savechild.identity.Survey.class, 1L);
        survey.removeQuestion(survey.getQuestions().get(0));
      });
      assertEquals(1, Database.count(emf, "Question"));
      assertNull(emf.callInTransaction(em -> em
          .createNativeQuery("select survey_id from question").getSingleResult()));
    }
  }

  @Test
  void sessionSaveNoLongerExists() {
    List<String> names = Arrays.stream(Session.class.getMethods()).map(m -> m.getName()).toList();
    assertFalse(names.contains("save"));
    assertFalse(names.contains("saveOrUpdate"));
    assertFalse(names.contains("update"));
    assertTrue(EntityManager.class.isAssignableFrom(Session.class));
    assertFalse(Arrays.stream(org.hibernate.annotations.CascadeType.values()).anyMatch(c -> c.name().equals("SAVE_UPDATE")));
    assertEquals(void.class, returnTypeOf("persist"));
    assertTrue(names.contains("persist"));
    assertTrue(names.contains("merge"));
  }
}
