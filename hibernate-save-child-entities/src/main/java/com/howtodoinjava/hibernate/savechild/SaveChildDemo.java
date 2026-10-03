package com.howtodoinjava.hibernate.savechild;

import jakarta.persistence.EntityManagerFactory;

public class SaveChildDemo {

  public static void main(String[] args) {
    try (EntityManagerFactory emf = Database.create(true, new SqlLog())) {

      step("1. Persist a new survey with three questions (cascade PERSIST)");
      Long surveyId = emf.callInTransaction(em -> {
        Survey survey = new Survey("Coffee Habits");
        survey.addQuestion(new Question("How many cups a day?", 1));
        survey.addQuestion(new Question("Favorite drink?", 2));
        survey.addQuestion(new Question("Milk or black?", 3));
        em.persist(survey);
        System.out.println("-- after persist(): survey id = " + survey.getId()
            + ", first question id = " + survey.getQuestions().get(0).getId());
        System.out.println("-- commit");
        return survey.getId();
      });
      rows(emf);

      step("2. Add a question to a managed survey (no persist call)");
      emf.runInTransaction(em -> {
        Survey survey = em.find(Survey.class, surveyId);
        survey.addQuestion(new Question("Tea or coffee?", 4));
        System.out.println("-- commit");
      });
      rows(emf);

      step("3. Add a question to a detached survey and merge it");
      Survey detached = emf.callInTransaction(em -> em
          .createQuery("select s from Survey s join fetch s.questions where s.id = :id", Survey.class)
          .setParameter("id", surveyId)
          .getSingleResult());
      Question sugar = new Question("Sugar?", 5);
      detached.addQuestion(sugar);
      Survey merged = emf.callInTransaction(em -> {
        Survey managed = em.merge(detached);
        System.out.println("-- commit");
        return managed;
      });
      System.out.println("-- sugar.getId() = " + sugar.getId()
          + ", merged copy id = " + merged.getQuestions().get(4).getId());
      rows(emf);

      step("4. persist() a detached survey");
      try {
        emf.runInTransaction(em -> em.persist(detached));
      } catch (RuntimeException e) {
        print(e);
      }

      step("5. Save a question for an existing survey with getReference()");
      emf.runInTransaction(em -> {
        Question q = new Question("Decaf?", 6);
        q.setSurvey(em.getReference(Survey.class, surveyId));
        em.persist(q);
        System.out.println("-- commit");
      });
      rows(emf);

      step("6. Add to the list only, without setting question.survey");
      emf.runInTransaction(em -> {
        Survey survey = new Survey("Weekend Plans");
        survey.getQuestions().add(new Question("Beach or hills?", 1));
        em.persist(survey);
      });
      System.out.println("-- " + emf.callInTransaction(em -> em
          .createNativeQuery("select text, survey_id from question where text = 'Beach or hills?'")
          .getResultList().stream().map(r -> java.util.Arrays.toString((Object[]) r)).toList()));
    }

    NoCascadeDemo.run();
  }

  static void rows(EntityManagerFactory emf) {
    System.out.println("-- surveys = " + Database.count(emf, "Survey")
        + ", questions = " + Database.count(emf, "Question"));
  }

  static void print(Throwable e) {
    System.out.println(e.getClass().getName() + ": " + e.getMessage());
    for (Throwable c = e.getCause(); c != null; c = c.getCause()) {
      System.out.println("  caused by " + c.getClass().getName() + ": " + c.getMessage());
    }
  }

  static void step(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
  }
}
