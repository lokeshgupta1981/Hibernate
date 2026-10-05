package com.howtodoinjava.hibernate.associations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PersistenceUnitUtil;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AssociationMappingsTest {

  private EntityManagerFactory emf;
  private Long memberId;

  @BeforeEach
  void setUp() {
    emf = Database.create(false);
    memberId = emf.callInTransaction(em -> {
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
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  private long count(String entity) {
    return Database.count(emf, entity);
  }

  private long rows(String table) {
    return ((Number) Database.scalar(emf, "select count(*) from " + table)).longValue();
  }

  private List<String> columns(String table) {
    return emf.callInTransaction(em -> em.createNativeQuery(
            "select column_name from information_schema.columns where table_name = ?1 "
                + "order by column_name", String.class)
        .setParameter(1, table.toUpperCase())
        .getResultList());
  }

  @Test
  void foreignKeysLiveOnTheOwningSide() {
    assertEquals(List.of("ID", "MEMBER_ID", "PLAN"), columns("MembershipCard"));
    assertEquals(List.of("ACTIVITY", "ID", "MEMBER_ID", "MINUTES"), columns("Workout"));
    assertEquals(List.of("ID", "NAME"), columns("Member"));
    assertEquals(List.of("CLASS_ID", "MEMBER_ID"), columns("member_class"));
    assertEquals(List.of("ID", "NAME"), columns("FitnessClass"));
  }

  @Test
  void unidirectionalOneToManyCreatesJoinTable() {
    assertEquals(List.of("CLASSES_ID", "TRAINER_ID"), columns("Trainer_FitnessClass"));
    assertEquals(2, rows("Trainer_FitnessClass"));
  }

  @Test
  void persistCascadesToCardAndWorkouts() {
    assertEquals(1, count("Member"));
    assertEquals(1, count("MembershipCard"));
    assertEquals(2, count("Workout"));
    assertEquals(1, rows("member_class"));
  }

  @Test
  void defaultFetchTypes() {
    PersistenceUnitUtil util = emf.getPersistenceUnitUtil();
    emf.runInTransaction(em -> {
      Member member = em.find(Member.class, memberId);
      assertTrue(util.isLoaded(member, "card"));        // @OneToOne: EAGER
      assertFalse(util.isLoaded(member, "workouts"));   // @OneToMany: LAZY
      assertFalse(util.isLoaded(member, "classes"));    // @ManyToMany: LAZY
      assertEquals(2, member.getWorkouts().size());
      assertEquals(1, member.getClasses().size());
    });
    emf.runInTransaction(em -> {
      MembershipCard card = em.createQuery("from MembershipCard", MembershipCard.class)
          .getSingleResult();
      assertTrue(util.isLoaded(card, "member"));        // @OneToOne: EAGER
    });
    emf.runInTransaction(em -> {
      Workout workout = em.createQuery("from Workout where activity = 'Running'", Workout.class)
          .getSingleResult();
      assertFalse(util.isLoaded(workout, "member"));    // @ManyToOne with LAZY
    });
  }

  @Test
  void annotationDefaultFetchTypes() throws Exception {
    assertEquals(FetchType.EAGER, OneToOne.class.getMethod("fetch").getDefaultValue());
    assertEquals(FetchType.EAGER, ManyToOne.class.getMethod("fetch").getDefaultValue());
    assertEquals(FetchType.LAZY, OneToMany.class.getMethod("fetch").getDefaultValue());
    assertEquals(FetchType.LAZY, ManyToMany.class.getMethod("fetch").getDefaultValue());
  }

  @Test
  void eagerOneToOneRunsOneSelectPerCard() {
    emf.runInTransaction(em -> {
      for (String name : List.of("Alex", "Maria")) {
        Member member = new Member(name);
        member.setCard(new MembershipCard("Silver"));
        em.persist(member);
      }
    });
    Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
    stats.clear();
    List<MembershipCard> cards = emf.callInTransaction(em ->
        em.createQuery("from MembershipCard", MembershipCard.class).getResultList());
    assertEquals(3, cards.size());
    assertEquals(4, stats.getPrepareStatementCount());   // 1 for the cards + 1 per member
  }

  @Test
  void joinFetchLoadsCardsAndMembersInOneStatement() {
    emf.runInTransaction(em -> {
      for (String name : List.of("Alex", "Maria")) {
        Member member = new Member(name);
        member.setCard(new MembershipCard("Silver"));
        em.persist(member);
      }
    });
    Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
    stats.clear();
    List<MembershipCard> cards = emf.callInTransaction(em ->
        em.createQuery("from MembershipCard c join fetch c.member", MembershipCard.class)
            .getResultList());
    assertEquals(3, cards.size());
    assertEquals(1, stats.getPrepareStatementCount());
  }

  @Test
  void queryWorkoutsFromTheOwningSide() {
    List<String> activities = emf.callInTransaction(em -> em.createQuery(
            "select w.activity from Workout w where w.member.id = :id order by w.activity",
            String.class)
        .setParameter("id", memberId)
        .getResultList());
    assertEquals(List.of("Rowing", "Running"), activities);
  }

  @Test
  void bothSidesNavigable() {
    emf.runInTransaction(em -> {
      Member member = em.find(Member.class, memberId);
      assertEquals("Gold", member.getCard().getPlan());
      assertEquals("Lokesh", member.getCard().getMember().getName());
      FitnessClass yoga = member.getClasses().iterator().next();
      assertEquals("Yoga", yoga.getName());
      assertEquals(1, yoga.getMembers().size());
    });
  }

  @Test
  void removingWorkoutDeletesIt() {
    emf.runInTransaction(em -> {
      Member member = em.find(Member.class, memberId);
      Workout rowing = member.getWorkouts().stream()
          .filter(w -> w.getActivity().equals("Rowing")).findFirst().orElseThrow();
      member.removeWorkout(rowing);
    });
    assertEquals(1, count("Workout"));
  }

  @Test
  void leavingClassDeletesOnlyLinkRow() {
    emf.runInTransaction(em -> {
      Member member = em.find(Member.class, memberId);
      member.leaveClass(member.getClasses().iterator().next());
    });
    assertEquals(0, rows("member_class"));
    assertEquals(2, count("FitnessClass"));
  }

  @Test
  void owningSideAloneWritesForeignKey() {
    emf.runInTransaction(em -> {
      Workout cycling = new Workout("Cycling", 45);
      cycling.setMember(em.find(Member.class, memberId));   // owning side only
      em.persist(cycling);
    });
    emf.runInTransaction(em ->
        assertEquals(3, em.find(Member.class, memberId).getWorkouts().size()));
  }

  @Test
  void inverseSideAloneLeavesForeignKeyNull() {
    emf.runInTransaction(em -> {
      Member member = em.find(Member.class, memberId);
      member.getWorkouts().add(new Workout("Cycling", 45));
      em.persist(member.getWorkouts().getLast());
    });
    assertNull(Database.scalar(emf, "select member_id from Workout where activity = 'Cycling'"));
    emf.runInTransaction(em ->
        assertEquals(2, em.find(Member.class, memberId).getWorkouts().size()));
  }

  @Test
  void inverseSideAloneWritesNoJoinRow() {
    emf.runInTransaction(em -> {
      Member member = em.find(Member.class, memberId);
      FitnessClass spinning = em.createQuery("from FitnessClass where name = 'Spinning'",
          FitnessClass.class).getSingleResult();
      spinning.getMembers().add(member);
    });
    assertEquals(1, rows("member_class"));
  }

  @Test
  void deletingMemberKeepsSharedClasses() {
    emf.runInTransaction(em -> em.remove(em.find(Member.class, memberId)));
    assertEquals(0, count("Member"));
    assertEquals(0, count("MembershipCard"));
    assertEquals(0, count("Workout"));
    assertEquals(0, rows("member_class"));
    assertEquals(2, count("FitnessClass"));
  }
}
