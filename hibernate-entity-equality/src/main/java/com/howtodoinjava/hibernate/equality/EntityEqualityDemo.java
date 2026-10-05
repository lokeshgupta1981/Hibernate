package com.howtodoinjava.hibernate.equality;

import jakarta.persistence.EntityManagerFactory;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.Hibernate;

public class EntityEqualityDemo {

  public static void main(String[] args) {
    defaultEquals();
    naiveIdEquals();
    idEquals();
    emailEquals();
    uuidEquals();
    proxies();
    lombokData();
    lombokFixed();
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("== " + title);
  }

  private static void defaultEquals() {
    try (EntityManagerFactory emf = Database.create("demo_plain", true,
        com.howtodoinjava.hibernate.equality.plain.Member.class)) {
      step("1. Default equals(): persist Lokesh");
      var lokesh = new com.howtodoinjava.hibernate.equality.plain.Member("lokesh@mail.com", "Lokesh");
      emf.runInTransaction(em -> em.persist(lokesh));
      Long id = lokesh.getId();

      step("2. Same session: find() twice");
      emf.runInTransaction(em -> {
        var a = em.find(com.howtodoinjava.hibernate.equality.plain.Member.class, id);
        var b = em.find(com.howtodoinjava.hibernate.equality.plain.Member.class, id);
        System.out.println("a == b      : " + (a == b));
        System.out.println("a.equals(b) : " + a.equals(b));
      });

      step("3. Different sessions: one find() in each");
      var a = emf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.equality.plain.Member.class, id));
      var b = emf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.equality.plain.Member.class, id));
      System.out.println("a == b      : " + (a == b));
      System.out.println("a.equals(b) : " + a.equals(b));

      step("4. HashSet with both copies");
      Set<com.howtodoinjava.hibernate.equality.plain.Member> members = new HashSet<>();
      members.add(a);
      members.add(b);
      var c = emf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.equality.plain.Member.class, id));
      System.out.println("size        : " + members.size());
      System.out.println("contains(c) : " + members.contains(c));
    }
  }

  private static void naiveIdEquals() {
    try (EntityManagerFactory emf = Database.create("demo_naive", true,
        com.howtodoinjava.hibernate.equality.naive.Member.class)) {
      step("5. Objects.hash(id): two new members");
      var lokesh = new com.howtodoinjava.hibernate.equality.naive.Member("lokesh@mail.com", "Lokesh");
      var alex = new com.howtodoinjava.hibernate.equality.naive.Member("alex@mail.com", "Alex");
      System.out.println("lokesh.equals(alex) : " + lokesh.equals(alex));
      Set<com.howtodoinjava.hibernate.equality.naive.Member> pair = new HashSet<>();
      pair.add(lokesh);
      pair.add(alex);
      System.out.println("set size            : " + pair.size());

      step("6. Objects.hash(id): add to a HashSet, then persist");
      Set<com.howtodoinjava.hibernate.equality.naive.Member> members = new HashSet<>();
      members.add(lokesh);
      System.out.println("hashCode before     : " + lokesh.hashCode());
      emf.runInTransaction(em -> em.persist(lokesh));
      System.out.println("id                  : " + lokesh.getId());
      System.out.println("hashCode after      : " + lokesh.hashCode());
      System.out.println("contains(lokesh)    : " + members.contains(lokesh));
      System.out.println("remove(lokesh)      : " + members.remove(lokesh));
    }
  }

  private static void idEquals() {
    try (EntityManagerFactory emf = Database.create("demo_byid", true,
        com.howtodoinjava.hibernate.equality.byid.Member.class)) {
      step("7. Id equals with constant hashCode: add, then persist");
      var lokesh = new com.howtodoinjava.hibernate.equality.byid.Member("lokesh@mail.com", "Lokesh");
      var alex = new com.howtodoinjava.hibernate.equality.byid.Member("alex@mail.com", "Alex");
      System.out.println("lokesh.equals(alex) : " + lokesh.equals(alex));
      Set<com.howtodoinjava.hibernate.equality.byid.Member> members = new HashSet<>();
      members.add(lokesh);
      emf.runInTransaction(em -> em.persist(lokesh));
      System.out.println("contains(lokesh)    : " + members.contains(lokesh));

      step("8. Id equals: copies from two sessions");
      Long id = lokesh.getId();
      var a = emf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.equality.byid.Member.class, id));
      var b = emf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.equality.byid.Member.class, id));
      System.out.println("a == b      : " + (a == b));
      System.out.println("a.equals(b) : " + a.equals(b));

      step("9. Id equals: loaded member vs proxy");
      emf.runInTransaction(em -> {
        var proxy = em.getReference(com.howtodoinjava.hibernate.equality.byid.Member.class, id);
        System.out.println("a.equals(proxy) : " + a.equals(proxy));
      });
    }
  }

  private static void emailEquals() {
    try (EntityManagerFactory emf = Database.create("demo_email", true,
        com.howtodoinjava.hibernate.equality.email.Member.class)) {
      step("10. Email equals: persist Lokesh");
      var lokesh = new com.howtodoinjava.hibernate.equality.email.Member("lokesh@mail.com", "Lokesh");
      Set<com.howtodoinjava.hibernate.equality.email.Member> members = new HashSet<>();
      members.add(lokesh);
      emf.runInTransaction(em -> em.persist(lokesh));
      System.out.println("contains(lokesh) : " + members.contains(lokesh));

      step("11. Email equals: loaded member vs a new object with the same email");
      var loaded = emf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.equality.email.Member.class, lokesh.getId()));
      var fromForm = new com.howtodoinjava.hibernate.equality.email.Member("lokesh@mail.com", "Lokesh G");
      System.out.println("fromForm.getId()        : " + fromForm.getId());
      System.out.println("fromForm.equals(loaded) : " + fromForm.equals(loaded));
    }
  }

  private static void uuidEquals() {
    try (EntityManagerFactory emf = Database.create("demo_uuid", true,
        com.howtodoinjava.hibernate.equality.uuid.Member.class)) {
      step("12. UUID id: assigned at creation");
      var lokesh = new com.howtodoinjava.hibernate.equality.uuid.Member("lokesh@mail.com", "Lokesh");
      System.out.println("id before persist : " + (lokesh.getId() != null));
      Set<com.howtodoinjava.hibernate.equality.uuid.Member> members = new HashSet<>();
      members.add(lokesh);
      emf.runInTransaction(em -> em.persist(lokesh));
      System.out.println("contains(lokesh)  : " + members.contains(lokesh));

      step("13. UUID id: merge() of a new member");
      emf.runInTransaction(em -> em.merge(new com.howtodoinjava.hibernate.equality.uuid.Member("alex@mail.com", "Alex")));
    }
  }

  private static void proxies() {
    try (EntityManagerFactory emf = Database.create("demo_proxy", true,
        com.howtodoinjava.hibernate.equality.proxy.Member.class)) {
      step("14. getClass() equals: persist and load Lokesh");
      var lokesh = new com.howtodoinjava.hibernate.equality.proxy.Member("lokesh@mail.com", "Lokesh");
      emf.runInTransaction(em -> em.persist(lokesh));
      Long id = lokesh.getId();
      var loaded = emf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.equality.proxy.Member.class, id));

      step("15. getClass() equals: loaded member vs proxy");
      emf.runInTransaction(em -> {
        var proxy = em.getReference(com.howtodoinjava.hibernate.equality.proxy.Member.class, id);
        System.out.println("proxy.getClass()           : " + proxy.getClass().getName());
        System.out.println("proxy instanceof Member    : " + (proxy instanceof com.howtodoinjava.hibernate.equality.proxy.Member));
        System.out.println("loaded.equals(proxy)       : " + loaded.equals(proxy));
        System.out.println("Hibernate.getClassLazy()   : " + Hibernate.getClassLazy(proxy).getSimpleName());
        System.out.println("initialized                : " + Hibernate.isInitialized(proxy));
        System.out.println("Hibernate.getClass()       : " + Hibernate.getClass(proxy).getSimpleName());
        System.out.println("initialized                : " + Hibernate.isInitialized(proxy));
      });
    }
  }

  private static void lombokData() {
    try (EntityManagerFactory emf = Database.create("demo_lombok", true,
        com.howtodoinjava.hibernate.equality.lombok.Member.class,
        com.howtodoinjava.hibernate.equality.lombok.Visit.class)) {
      step("16. Lombok @Data: add to a HashSet, then persist");
      var lokesh = new com.howtodoinjava.hibernate.equality.lombok.Member("lokesh@mail.com", "Lokesh");
      Set<com.howtodoinjava.hibernate.equality.lombok.Member> members = new HashSet<>();
      members.add(lokesh);
      emf.runInTransaction(em -> em.persist(lokesh));
      System.out.println("contains(lokesh) : " + members.contains(lokesh));

      step("17. Lombok @Data: toString() of a detached member");
      var detached = emf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.equality.lombok.Member.class, lokesh.getId()));
      try {
        System.out.println(detached.toString());
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }

      step("18. Lombok @Data: hashCode() with a bidirectional association");
      var alex = new com.howtodoinjava.hibernate.equality.lombok.Member("alex@mail.com", "Alex");
      alex.addVisit(new com.howtodoinjava.hibernate.equality.lombok.Visit("Monday"));
      try {
        alex.hashCode();
      } catch (StackOverflowError e) {
        System.out.println("java.lang.StackOverflowError");
      }
    }
  }

  private static void lombokFixed() {
    try (EntityManagerFactory emf = Database.create("demo_lombokfixed", true,
        com.howtodoinjava.hibernate.equality.lombokfixed.Member.class,
        com.howtodoinjava.hibernate.equality.lombokfixed.Visit.class)) {
      step("19. Lombok with explicit includes");
      var lokesh = new com.howtodoinjava.hibernate.equality.lombokfixed.Member("lokesh@mail.com", "Lokesh");
      lokesh.addVisit(new com.howtodoinjava.hibernate.equality.lombokfixed.Visit("Monday"));
      emf.runInTransaction(em -> {
        em.persist(lokesh);
        em.persist(lokesh.getVisits().get(0));
      });
      var detached = emf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.equality.lombokfixed.Member.class, lokesh.getId()));
      System.out.println("toString()       : " + detached);
      System.out.println("equals(new copy) : " + detached.equals(
          new com.howtodoinjava.hibernate.equality.lombokfixed.Member("lokesh@mail.com", "Lokesh")));
    }
  }
}
