package com.howtodoinjava.hibernate.savechild;

import com.howtodoinjava.hibernate.savechild.nocascade.Question;
import com.howtodoinjava.hibernate.savechild.nocascade.Survey;
import jakarta.persistence.EntityManagerFactory;

/** The same model WITHOUT cascade, plus the unidirectional and IDENTITY variants. */
public class NoCascadeDemo {

  static void run() {
    try (EntityManagerFactory emf = Database.create(true, new SqlLog(), Survey.class, Question.class)) {

      SaveChildDemo.step("7. No cascade: persist the survey only");
      emf.runInTransaction(em -> {
        Survey survey = new Survey("Coffee Habits");
        survey.addQuestion(new Question("How many cups a day?", 1));
        em.persist(survey);
      });
      System.out.println("-- surveys = " + Database.count(emf, "Survey")
          + ", questions = " + Database.count(emf, "Question"));

      SaveChildDemo.step("8. No cascade: persist the question only");
      try {
        emf.runInTransaction(em -> {
          Survey survey = new Survey("Coffee Habits");
          Question q = new Question("Favorite drink?", 2);
          survey.addQuestion(q);
          em.persist(q);
        });
      } catch (RuntimeException e) {
        SaveChildDemo.print(e);
      }

      SaveChildDemo.step("9. No cascade: persist both, child first");
      emf.runInTransaction(em -> {
        Survey survey = new Survey("Tea Habits");
        Question q = new Question("Green or black?", 1);
        survey.addQuestion(q);
        em.persist(q);
        em.persist(survey);
      });

      SaveChildDemo.step("10. No cascade: persist both, parent first");
      Long surveyId = emf.callInTransaction(em -> {
        Survey survey = new Survey("Lunch Habits");
        Question q = new Question("Home or office?", 1);
        survey.addQuestion(q);
        em.persist(survey);
        em.persist(q);
        return survey.getId();
      });

      SaveChildDemo.step("11. No cascade: merge a detached survey with a new question");
      Survey detached = emf.callInTransaction(em -> em
          .createQuery("select s from Survey s join fetch s.questions where s.id = :id", Survey.class)
          .setParameter("id", surveyId)
          .getSingleResult());
      detached.addQuestion(new Question("Dessert?", 2));
      emf.runInTransaction(em -> em.merge(detached));
      System.out.println("-- questions = " + Database.count(emf, "Question"));
    }

    try (EntityManagerFactory emf = Database.create(true, new SqlLog(),
        com.howtodoinjava.hibernate.savechild.unidirectional.Survey.class,
        com.howtodoinjava.hibernate.savechild.unidirectional.Question.class)) {
      SaveChildDemo.step("12. Unidirectional @OneToMany with @JoinColumn");
      emf.runInTransaction(em -> {
        var survey = new com.howtodoinjava.hibernate.savechild.unidirectional.Survey("Coffee Habits");
        survey.getQuestions().add(new com.howtodoinjava.hibernate.savechild.unidirectional.Question("How many cups a day?", 1));
        survey.getQuestions().add(new com.howtodoinjava.hibernate.savechild.unidirectional.Question("Favorite drink?", 2));
        em.persist(survey);
      });
    }

    try (EntityManagerFactory emf = Database.create(true, new SqlLog(),
        com.howtodoinjava.hibernate.savechild.identity.Survey.class,
        com.howtodoinjava.hibernate.savechild.identity.Question.class)) {
      SaveChildDemo.step("13. GenerationType.IDENTITY with CascadeType.ALL");
      emf.runInTransaction(em -> {
        var survey = new com.howtodoinjava.hibernate.savechild.identity.Survey("Coffee Habits");
        survey.addQuestion(new com.howtodoinjava.hibernate.savechild.identity.Question("How many cups a day?", 1));
        survey.addQuestion(new com.howtodoinjava.hibernate.savechild.identity.Question("Favorite drink?", 2));
        System.out.println("-- before persist()");
        em.persist(survey);
        System.out.println("-- after persist(): survey id = " + survey.getId());
        System.out.println("-- commit");
      });
    }
  }
}
