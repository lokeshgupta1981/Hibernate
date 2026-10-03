package com.howtodoinjava.hibernate.helloworld;

import java.time.LocalDate;
import java.util.List;
import org.hibernate.SessionFactory;

public class HelloWorldDemo {

  public static void main(String[] args) {
    step("1. Build the SessionFactory (creates the table)");
    try (SessionFactory sessionFactory = Database.create(true)) {

      step("2. Save two notes");
      Long id = sessionFactory.callInTransaction(em -> {
        Note groceries = new Note("Groceries", "Milk, eggs, bread", LocalDate.of(2026, 10, 3));
        em.persist(groceries);
        em.persist(new Note("Ideas", "Learn Hibernate", LocalDate.of(2026, 10, 3)));
        return groceries.getId();
      });
      System.out.println("generated id = " + id);

      step("3. Read a note by id");
      Note note = sessionFactory.callInTransaction(em -> em.find(Note.class, id));
      System.out.println(note);

      step("4. Read all notes with a query");
      List<Note> notes = sessionFactory.callInTransaction(em ->
          em.createQuery("from Note order by title", Note.class).getResultList());
      notes.forEach(System.out::println);

      step("5. Update the note");
      sessionFactory.runInTransaction(em -> {
        Note loaded = em.find(Note.class, id);
        loaded.setText("Milk, eggs, bread, coffee");
      });
      Note updated = sessionFactory.callInTransaction(em -> em.find(Note.class, id));
      System.out.println(updated);

      step("6. Update a detached note with merge()");
      updated.setTitle("Shopping");
      sessionFactory.runInTransaction(em -> em.merge(updated));

      step("7. Delete the note");
      sessionFactory.runInTransaction(em -> em.remove(em.find(Note.class, id)));
      Note deleted = sessionFactory.callInTransaction(em -> em.find(Note.class, id));
      System.out.println("after delete: " + deleted);

      step("8. The same work with the Hibernate Session API");
      Long sessionId = sessionFactory.fromTransaction(session -> {
        Note todo = new Note("Todo", "Call the bank", LocalDate.of(2026, 10, 4));
        session.persist(todo);
        return todo.getId();
      });
      sessionFactory.inTransaction(session -> System.out.println(session.find(Note.class, sessionId)));

      step("9. Close the SessionFactory (drops the table)");
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
  }
}
