package com.howtodoinjava.hibernate.associations;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
import java.util.List;

public class AssociationMappingsDemo {

  public static void main(String[] args) {
    try (EntityManagerFactory emf = Database.create(true)) {

      step("1. Save classes, a trainer, and a member with a card, two workouts and one class");
      Long memberId = emf.callInTransaction(em -> {
        FitnessClass yoga = new FitnessClass("Yoga");
        FitnessClass spinning = new FitnessClass("Spinning");
        em.persist(yoga);
        em.persist(spinning);

        Trainer alex = new Trainer("Alex");
        alex.getClasses().add(yoga);
        alex.getClasses().add(spinning);
        em.persist(alex);

        Member lokesh = new Member("Lokesh");
        lokesh.setCard(new MembershipCard("Gold"));
        lokesh.addWorkout(new Workout("Running", 30));
        lokesh.addWorkout(new Workout("Rowing", 20));
        lokesh.joinClass(yoga);
        em.persist(lokesh);
        return lokesh.getId();
      });

      step("2. Load the member: which associations are loaded?");
      emf.runInTransaction(em -> {
        PersistenceUnitUtil util = emf.getPersistenceUnitUtil();
        Member member = em.find(Member.class, memberId);
        System.out.println("card loaded:     " + util.isLoaded(member, "card"));
        System.out.println("workouts loaded: " + util.isLoaded(member, "workouts"));
        System.out.println("classes loaded:  " + util.isLoaded(member, "classes"));
        System.out.println("workouts: " + member.getWorkouts().size());
        System.out.println("classes:  " + member.getClasses().size());
      });

      step("3. Query the cards: is the member loaded?");
      emf.runInTransaction(em -> {
        MembershipCard card = em.createQuery("from MembershipCard", MembershipCard.class)
            .getSingleResult();
        System.out.println("member loaded: "
            + emf.getPersistenceUnitUtil().isLoaded(card, "member"));
      });

      step("3b. Query the cards with join fetch");
      emf.runInTransaction(em -> em.createQuery("from MembershipCard c join fetch c.member",
          MembershipCard.class).getResultList());

      step("3c. Query workouts from the owning side");
      List<String> activities = emf.callInTransaction(em -> em.createQuery(
              "select w.activity from Workout w where w.member.id = :id order by w.activity",
              String.class)
          .setParameter("id", memberId)
          .getResultList());
      System.out.println("activities: " + activities);

      step("4. Remove the 'Rowing' workout (orphanRemoval = true)");
      emf.runInTransaction(em -> {
        Member member = em.find(Member.class, memberId);
        Workout rowing = member.getWorkouts().stream()
            .filter(w -> w.getActivity().equals("Rowing")).findFirst().orElseThrow();
        member.removeWorkout(rowing);
      });
      System.out.println("workouts: " + Database.count(emf, "Workout"));

      step("5. Join 'Spinning', then leave 'Yoga'");
      emf.runInTransaction(em -> {
        Member member = em.find(Member.class, memberId);
        FitnessClass spinning = em.createQuery("from FitnessClass where name = 'Spinning'",
            FitnessClass.class).getSingleResult();
        member.joinClass(spinning);
      });
      emf.runInTransaction(em -> {
        Member member = em.find(Member.class, memberId);
        FitnessClass yoga = member.getClasses().stream()
            .filter(c -> c.getName().equals("Yoga")).findFirst().orElseThrow();
        member.leaveClass(yoga);
      });
      System.out.println("member_class rows: " + Database.scalar(emf, "select count(*) from member_class"));
      System.out.println("classes: " + Database.count(emf, "FitnessClass"));

      step("6. Update only the inverse side: workouts.add() without setMember()");
      emf.runInTransaction(em -> {
        Member member = em.find(Member.class, memberId);
        member.getWorkouts().add(new Workout("Cycling", 45));
        em.persist(member.getWorkouts().getLast());
      });
      System.out.println("member_id of Cycling: " + Database.scalar(emf,
          "select member_id from Workout where activity = 'Cycling'"));

      step("7. Update only the inverse side: fitnessClass.getMembers().add()");
      emf.runInTransaction(em -> {
        Member member = em.find(Member.class, memberId);
        FitnessClass yoga = em.createQuery("from FitnessClass where name = 'Yoga'",
            FitnessClass.class).getSingleResult();
        yoga.getMembers().add(member);
      });
      System.out.println("member_class rows: " + Database.scalar(emf, "select count(*) from member_class"));

      step("8. Delete the member");
      emf.runInTransaction(em -> em.remove(em.find(Member.class, memberId)));
      System.out.println("members: " + Database.count(emf, "Member")
          + ", cards: " + Database.count(emf, "MembershipCard")
          + ", workouts: " + Database.count(emf, "Workout")
          + ", classes: " + Database.count(emf, "FitnessClass"));
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
  }
}
