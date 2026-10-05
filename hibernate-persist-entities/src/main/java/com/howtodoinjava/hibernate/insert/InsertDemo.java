package com.howtodoinjava.hibernate.insert;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.SessionFactory;

public class InsertDemo {

  public static void main(String[] args) {
    try (EntityManagerFactory emf = Database.create(true)) {

      step("1. persist() a Pet (SEQUENCE id)");
      Pet max = new Pet("Max", "dog", 3);
      emf.runInTransaction(em -> {
        System.out.println("-- before persist: id = " + max.getId() + ", contains = " + em.contains(max));
        em.persist(max);
        System.out.println("-- after persist: id = " + max.getId() + ", contains = " + em.contains(max));
        System.out.println("-- commit");
      });
      System.out.println("-- after commit: pets = " + Database.count(emf, "Pet"));

      step("2. persist() an Adopter (IDENTITY id)");
      Adopter lokesh = new Adopter("Lokesh");
      emf.runInTransaction(em -> {
        em.persist(lokesh);
        System.out.println("-- after persist: id = " + lokesh.getId());
        System.out.println("-- commit");
      });

      step("3. persist() a Kennel (assigned id)");
      emf.runInTransaction(em -> {
        em.persist(new Kennel(1, "large"));
        System.out.println("-- after persist");
        System.out.println("-- commit");
      });

      step("4. flush() sends the INSERT before commit");
      emf.runInTransaction(em -> {
        Pet luna = new Pet("Luna", "cat", 2);
        em.persist(luna);
        System.out.println("-- after persist");
        em.flush();
        System.out.println("-- after flush");
        System.out.println("-- commit");
      });

      step("5. A query flushes pending inserts first");
      emf.runInTransaction(em -> {
        em.persist(new Pet("Coco", "rabbit", 1));
        System.out.println("-- after persist");
        long cats = em.createQuery("select count(*) from Pet", Long.class).getSingleResult();
        System.out.println("-- pets = " + cats);
      });

      step("6. Get the generated id");
      Long id = emf.callInTransaction(em -> {
        Pet daisy = new Pet("Daisy", "dog", 5);
        em.persist(daisy);
        return daisy.getId();
      });
      Object id2 = emf.getPersistenceUnitUtil().getIdentifier(max);
      System.out.println("-- daisy id = " + id + ", max id from PersistenceUnitUtil = " + id2);

      step("7. persist() a detached entity");
      try {
        emf.runInTransaction(em -> {
          max.setAge(4);
          em.persist(max);
        });
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }

      step("8. merge() a detached entity");
      max.setAge(4);
      Pet managed = emf.callInTransaction(em -> {
        Pet copy = em.merge(max);
        System.out.println("-- copy == max: " + (copy == max) + ", contains(copy) = " + em.contains(copy)
            + ", contains(max) = " + em.contains(max));
        return copy;
      });
      System.out.println("-- age in database = " + emf.callInTransaction(em -> em.find(Pet.class, max.getId()).getAge()));

      step("9. merge() a new entity");
      Pet bella = new Pet("Bella", "cat", 1);
      Pet bellaCopy = emf.callInTransaction(em -> em.merge(bella));
      System.out.println("-- bella.getId() = " + bella.getId() + ", copy id = " + bellaCopy.getId());

      step("10. Duplicate id in the same persistence context");
      try {
        emf.runInTransaction(em -> {
          em.persist(new Kennel(2, "small"));
          em.persist(new Kennel(2, "medium"));
        });
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }

      step("11. Duplicate id already in the database");
      try {
        emf.runInTransaction(em -> em.persist(new Kennel(1, "small")));
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
        System.out.println("   cause: " + e.getCause().getClass().getName());
      }

      step("12. HQL insert ... values");
      int rows = emf.callInTransaction(em -> em.createQuery("""
              insert Pet (name, species, age, adopted)
              values ('Rocky', 'dog', 4, false), ('Milo', 'cat', 3, false)""")
          .executeUpdate());
      System.out.println("-- rows = " + rows + ", pets = " + Database.count(emf, "Pet"));

      step("13. HQL insert ... select");
      emf.runInTransaction(em -> em.find(Pet.class, max.getId()).setAdopted(true));
      emf.runInTransaction(em -> em.find(Pet.class, id).setAdopted(true));
      int archived = emf.callInTransaction(em -> em.createQuery("""
              insert into AdoptedPet (name, species)
              select p.name, p.species from Pet p where p.adopted = true""")
          .executeUpdate());
      System.out.println("-- rows = " + archived + ", names = " + emf.callInTransaction(em -> em
          .createQuery("select name from AdoptedPet order by name", String.class).getResultList()));

      step("14. Native SQL insert");
      int nativeRows = emf.callInTransaction(em -> em
          .createNativeQuery("insert into Kennel (number, size) values (?, ?)")
          .setParameter(1, 3)
          .setParameter(2, "medium")
          .executeUpdate());
      System.out.println("-- rows = " + nativeRows + ", kennels = " + Database.count(emf, "Kennel"));

      step("15. Session.persist() and StatelessSession.insert()");
      emf.runInTransaction(em -> em.unwrap(Session.class).persist(new Pet("Oscar", "parrot", 6)));
      SessionFactory sf = emf.unwrap(SessionFactory.class);
      Object oliverId = sf.fromStatelessTransaction(ss -> ss.insert(new Pet("Oliver", "cat", 2)));
      System.out.println("-- insert() returned id = " + oliverId);

      step("16. persist() without a transaction");
      try (EntityManager em = emf.createEntityManager()) {
        em.persist(new Pet("Ghost", "dog", 1));
        em.persist(new Adopter("Ghost"));
        System.out.println("-- after persist");
      }
      System.out.println("-- Ghost rows = " + emf.callInTransaction(em -> em
          .createQuery("select count(*) from Pet where name = 'Ghost'", Long.class).getSingleResult())
          + ", Ghost adopters = " + emf.callInTransaction(em -> em
          .createQuery("select count(*) from Adopter where name = 'Ghost'", Long.class).getSingleResult()));

      step("17. Rollback discards the INSERT");
      try {
        emf.runInTransaction(em -> {
          em.persist(new Adopter("Anna"));
          throw new IllegalStateException("adoption form incomplete");
        });
      } catch (IllegalStateException e) {
        System.out.println("-- " + e.getMessage());
      }
      System.out.println("-- adopters = " + Database.count(emf, "Adopter"));
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
  }
}
