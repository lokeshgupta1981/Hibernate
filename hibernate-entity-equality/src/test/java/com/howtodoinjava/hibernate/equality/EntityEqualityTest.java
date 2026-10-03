package com.howtodoinjava.hibernate.equality;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EntityEqualityTest {

  static EntityManagerFactory plainEmf;
  static EntityManagerFactory naiveEmf;
  static EntityManagerFactory byIdEmf;
  static EntityManagerFactory emailEmf;
  static EntityManagerFactory uuidEmf;
  static EntityManagerFactory proxyEmf;
  static EntityManagerFactory lombokEmf;
  static EntityManagerFactory lombokFixedEmf;

  @BeforeAll
  static void setUp() {
    plainEmf = Database.create("test_plain", false, com.howtodoinjava.hibernate.equality.plain.Member.class);
    naiveEmf = Database.create("test_naive", false, com.howtodoinjava.hibernate.equality.naive.Member.class);
    byIdEmf = Database.create("test_byid", false, com.howtodoinjava.hibernate.equality.byid.Member.class);
    emailEmf = Database.create("test_email", false, com.howtodoinjava.hibernate.equality.email.Member.class);
    uuidEmf = Database.create("test_uuid", false, com.howtodoinjava.hibernate.equality.uuid.Member.class);
    proxyEmf = Database.create("test_proxy", false, com.howtodoinjava.hibernate.equality.proxy.Member.class);
    lombokEmf = Database.create("test_lombok", false,
        com.howtodoinjava.hibernate.equality.lombok.Member.class,
        com.howtodoinjava.hibernate.equality.lombok.Visit.class);
    lombokFixedEmf = Database.create("test_lombokfixed", false,
        com.howtodoinjava.hibernate.equality.lombokfixed.Member.class,
        com.howtodoinjava.hibernate.equality.lombokfixed.Visit.class);
  }

  @AfterAll
  static void tearDown() {
    for (EntityManagerFactory emf : new EntityManagerFactory[] {plainEmf, naiveEmf, byIdEmf, emailEmf,
        uuidEmf, proxyEmf, lombokEmf, lombokFixedEmf}) {
      emf.close();
    }
  }

  @Test
  void sessionSaveNoLongerExistsInHibernate7() {
    assertThrows(NoSuchMethodException.class, () -> org.hibernate.Session.class.getMethod("save", Object.class));
  }

  /** Default equals() and hashCode() from Object. */
  @Nested
  class DefaultEquals {

    private Long saveLokesh() {
      var lokesh = new com.howtodoinjava.hibernate.equality.plain.Member("lokesh@mail.com", "Lokesh");
      plainEmf.runInTransaction(em -> em.persist(lokesh));
      return lokesh.getId();
    }

    @Test
    void sameSessionReturnsSameInstanceWithOneSelect() {
      Long id = saveLokesh();
      Statistics stats = Database.statistics(plainEmf);
      plainEmf.runInTransaction(em -> {
        var a = em.find(com.howtodoinjava.hibernate.equality.plain.Member.class, id);
        var b = em.find(com.howtodoinjava.hibernate.equality.plain.Member.class, id);
        assertSame(a, b);
        assertTrue(a.equals(b));
      });
      assertEquals(1, stats.getPrepareStatementCount());
    }

    @Test
    void differentSessionsReturnDifferentInstances() {
      Long id = saveLokesh();
      Statistics stats = Database.statistics(plainEmf);
      var a = plainEmf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.equality.plain.Member.class, id));
      var b = plainEmf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.equality.plain.Member.class, id));
      assertNotSame(a, b);
      assertFalse(a.equals(b));
      assertEquals(2, stats.getPrepareStatementCount());
    }

    @Test
    void hashSetKeepsBothCopiesOfTheSameRow() {
      Long id = saveLokesh();
      var a = plainEmf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.equality.plain.Member.class, id));
      var b = plainEmf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.equality.plain.Member.class, id));
      var c = plainEmf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.equality.plain.Member.class, id));

      Set<com.howtodoinjava.hibernate.equality.plain.Member> members = new HashSet<>();
      members.add(a);
      members.add(b);
      assertEquals(2, members.size());
      assertFalse(members.contains(c));
      assertFalse(members.remove(c));
    }
  }

  /** equals() and hashCode() on the generated id with Objects.hash(id). */
  @Nested
  class NaiveIdEquals {

    @Test
    void twoNewMembersAreEqualBecauseBothIdsAreNull() {
      var lokesh = new com.howtodoinjava.hibernate.equality.naive.Member("lokesh@mail.com", "Lokesh");
      var alex = new com.howtodoinjava.hibernate.equality.naive.Member("alex@mail.com", "Alex");
      assertTrue(lokesh.equals(alex));

      Set<com.howtodoinjava.hibernate.equality.naive.Member> members = new HashSet<>();
      members.add(lokesh);
      members.add(alex);
      assertEquals(1, members.size());
    }

    @Test
    void hashSetLosesTheMemberAfterPersist() {
      var lokesh = new com.howtodoinjava.hibernate.equality.naive.Member("lokesh@mail.com", "Lokesh");
      Set<com.howtodoinjava.hibernate.equality.naive.Member> members = new HashSet<>();
      members.add(lokesh);
      assertEquals(31, lokesh.hashCode());          // Objects.hash(null)

      naiveEmf.runInTransaction(em -> em.persist(lokesh));

      assertEquals(31 + lokesh.getId().hashCode(), lokesh.hashCode());
      assertFalse(members.contains(lokesh));
      assertFalse(members.remove(lokesh));
      assertEquals(1, members.size());
    }
  }

  /** equals() on a non-null id, constant hashCode(). */
  @Nested
  class IdEquals {

    @Test
    void twoNewMembersAreNotEqual() {
      var lokesh = new com.howtodoinjava.hibernate.equality.byid.Member("lokesh@mail.com", "Lokesh");
      var alex = new com.howtodoinjava.hibernate.equality.byid.Member("alex@mail.com", "Alex");
      assertFalse(lokesh.equals(alex));

      Set<com.howtodoinjava.hibernate.equality.byid.Member> members = new HashSet<>();
      members.add(lokesh);
      members.add(alex);
      assertEquals(2, members.size());
    }

    @Test
    void hashSetFindsTheMemberAfterPersist() {
      var lokesh = new com.howtodoinjava.hibernate.equality.byid.Member("lokesh@mail.com", "Lokesh");
      Set<com.howtodoinjava.hibernate.equality.byid.Member> members = new HashSet<>();
      members.add(lokesh);
      int before = lokesh.hashCode();

      byIdEmf.runInTransaction(em -> em.persist(lokesh));

      assertEquals(before, lokesh.hashCode());
      assertTrue(members.contains(lokesh));
    }

    @Test
    void copiesFromDifferentSessionsAreEqual() {
      var lokesh = new com.howtodoinjava.hibernate.equality.byid.Member("lokesh@mail.com", "Lokesh");
      byIdEmf.runInTransaction(em -> em.persist(lokesh));
      Long id = lokesh.getId();

      var a = byIdEmf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.equality.byid.Member.class, id));
      var b = byIdEmf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.equality.byid.Member.class, id));
      assertNotSame(a, b);
      assertTrue(a.equals(b));

      Set<com.howtodoinjava.hibernate.equality.byid.Member> members = new HashSet<>();
      members.add(a);
      members.add(b);
      assertEquals(1, members.size());
    }

    @Test
    void proxyIsEqualToTheLoadedMember() {
      var lokesh = new com.howtodoinjava.hibernate.equality.byid.Member("lokesh@mail.com", "Lokesh");
      byIdEmf.runInTransaction(em -> em.persist(lokesh));
      Long id = lokesh.getId();
      var loaded = byIdEmf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.equality.byid.Member.class, id));

      byIdEmf.runInTransaction(em -> {
        var proxy = em.getReference(com.howtodoinjava.hibernate.equality.byid.Member.class, id);
        assertNotSame(com.howtodoinjava.hibernate.equality.byid.Member.class, proxy.getClass());
        assertTrue(loaded.equals(proxy));
        assertTrue(proxy.equals(loaded));
      });
    }
  }

  /** equals() and hashCode() on the membership email (business key). */
  @Nested
  class EmailEquals {

    @Test
    void newObjectEqualsTheLoadedMemberWithTheSameEmail() {
      var lokesh = new com.howtodoinjava.hibernate.equality.email.Member("lokesh.e@mail.com", "Lokesh");
      emailEmf.runInTransaction(em -> em.persist(lokesh));
      Long id = lokesh.getId();

      var loaded = emailEmf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.equality.email.Member.class, id));
      var fromForm = new com.howtodoinjava.hibernate.equality.email.Member("lokesh.e@mail.com", "Lokesh G");
      assertNull(fromForm.getId());
      assertTrue(fromForm.equals(loaded));
      assertEquals(fromForm.hashCode(), loaded.hashCode());
    }

    @Test
    void sameSessionReturnsSameInstanceAndOtherSessionAnEqualCopy() {
      var nina = new com.howtodoinjava.hibernate.equality.email.Member("nina.e@mail.com", "Nina");
      emailEmf.runInTransaction(em -> em.persist(nina));
      Long id = nina.getId();
      Statistics stats = Database.statistics(emailEmf);
      var c = emailEmf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.equality.email.Member.class, id));
      emailEmf.runInTransaction(em -> {
        var a = em.find(com.howtodoinjava.hibernate.equality.email.Member.class, id);
        var b = em.find(com.howtodoinjava.hibernate.equality.email.Member.class, id);
        assertSame(a, b);
        assertNotSame(a, c);
        assertTrue(a.equals(c));
      });
      assertEquals(2, stats.getPrepareStatementCount());
    }

    @Test
    void hashSetFindsTheMemberAfterPersist() {
      var alex = new com.howtodoinjava.hibernate.equality.email.Member("alex.e@mail.com", "Alex");
      Set<com.howtodoinjava.hibernate.equality.email.Member> members = new HashSet<>();
      members.add(alex);
      emailEmf.runInTransaction(em -> em.persist(alex));
      assertTrue(members.contains(alex));
    }

    @Test
    void copiesFromDifferentSessionsAreEqual() {
      var maria = new com.howtodoinjava.hibernate.equality.email.Member("maria.e@mail.com", "Maria");
      emailEmf.runInTransaction(em -> em.persist(maria));
      Long id = maria.getId();
      var a = emailEmf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.equality.email.Member.class, id));
      var b = emailEmf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.equality.email.Member.class, id));
      assertNotSame(a, b);
      assertTrue(a.equals(b));
    }

    @Test
    void differentEmailsAreNotEqual() {
      var lokesh = new com.howtodoinjava.hibernate.equality.email.Member("lokesh@mail.com", "Lokesh");
      var alex = new com.howtodoinjava.hibernate.equality.email.Member("alex@mail.com", "Lokesh");
      assertFalse(lokesh.equals(alex));
    }
  }

  /** UUID id assigned when the object is created. */
  @Nested
  class UuidEquals {

    @Test
    void idExistsBeforePersistAndHashCodeNeverChanges() {
      var lokesh = new com.howtodoinjava.hibernate.equality.uuid.Member("lokesh@mail.com", "Lokesh");
      UUID id = lokesh.getId();
      assertNotNull(id);
      int before = lokesh.hashCode();

      Set<com.howtodoinjava.hibernate.equality.uuid.Member> members = new HashSet<>();
      members.add(lokesh);
      uuidEmf.runInTransaction(em -> em.persist(lokesh));

      assertEquals(id, lokesh.getId());
      assertEquals(before, lokesh.hashCode());
      assertTrue(members.contains(lokesh));
    }

    @Test
    void copiesFromDifferentSessionsAreEqual() {
      var alex = new com.howtodoinjava.hibernate.equality.uuid.Member("alex@mail.com", "Alex");
      uuidEmf.runInTransaction(em -> em.persist(alex));
      var a = uuidEmf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.equality.uuid.Member.class, alex.getId()));
      var b = uuidEmf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.equality.uuid.Member.class, alex.getId()));
      assertNotSame(a, b);
      assertTrue(a.equals(b));
    }

    @Test
    void persistRunsOneInsertButMergeSelectsFirst() {
      Statistics stats = Database.statistics(uuidEmf);
      uuidEmf.runInTransaction(em -> em.persist(new com.howtodoinjava.hibernate.equality.uuid.Member("maria@mail.com", "Maria")));
      assertEquals(1, stats.getPrepareStatementCount());

      stats.clear();
      uuidEmf.runInTransaction(em -> em.merge(new com.howtodoinjava.hibernate.equality.uuid.Member("nina@mail.com", "Nina")));
      assertEquals(2, stats.getPrepareStatementCount());
    }
  }

  /** getClass() and field access in equals() against Hibernate proxies. */
  @Nested
  class Proxies {

    private Long saveLokesh() {
      var lokesh = new com.howtodoinjava.hibernate.equality.proxy.Member("lokesh@mail.com", "Lokesh");
      proxyEmf.runInTransaction(em -> em.persist(lokesh));
      return lokesh.getId();
    }

    @Test
    void getClassEqualsFailsForAProxy() {
      Long id = saveLokesh();
      var loaded = proxyEmf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.equality.proxy.Member.class, id));
      proxyEmf.runInTransaction(em -> {
        var proxy = em.getReference(com.howtodoinjava.hibernate.equality.proxy.Member.class, id);
        assertNotEquals(com.howtodoinjava.hibernate.equality.proxy.Member.class, proxy.getClass());
        assertTrue(proxy.getClass().getSimpleName().startsWith("Member$HibernateProxy"));
        assertTrue(proxy instanceof com.howtodoinjava.hibernate.equality.proxy.Member);
        assertFalse(loaded.equals(proxy));
      });
    }

    @Test
    void proxyFieldsAreNullButGettersWork() throws Exception {
      Long id = saveLokesh();
      Field idField = com.howtodoinjava.hibernate.equality.proxy.Member.class.getDeclaredField("id");
      Field emailField = com.howtodoinjava.hibernate.equality.proxy.Member.class.getDeclaredField("email");
      idField.setAccessible(true);
      emailField.setAccessible(true);
      proxyEmf.runInTransaction(em -> {
        var proxy = em.getReference(com.howtodoinjava.hibernate.equality.proxy.Member.class, id);
        try {
          assertEquals(id, proxy.getId());
          assertFalse(Hibernate.isInitialized(proxy));
          assertEquals("lokesh@mail.com", proxy.getEmail());
          assertTrue(Hibernate.isInitialized(proxy));
          assertNull(idField.get(proxy));
          assertNull(emailField.get(proxy));
        } catch (IllegalAccessException e) {
          throw new IllegalStateException(e);
        }
      });
    }

    @Test
    void hibernateGetClassLoadsTheProxyButGetClassLazyDoesNot() {
      Long id = saveLokesh();
      proxyEmf.runInTransaction(em -> {
        var proxy = em.getReference(com.howtodoinjava.hibernate.equality.proxy.Member.class, id);
        Statistics stats = Database.statistics(proxyEmf);

        assertEquals(com.howtodoinjava.hibernate.equality.proxy.Member.class, Hibernate.getClassLazy(proxy));
        assertFalse(Hibernate.isInitialized(proxy));
        assertEquals(0, stats.getPrepareStatementCount());

        assertEquals(com.howtodoinjava.hibernate.equality.proxy.Member.class, Hibernate.getClass(proxy));
        assertTrue(Hibernate.isInitialized(proxy));
        assertEquals(1, stats.getPrepareStatementCount());
      });
    }
  }

  /** Lombok @Data on entities. */
  @Nested
  class LombokData {

    @Test
    void hashCodeChangesAfterPersist() {
      var lokesh = new com.howtodoinjava.hibernate.equality.lombok.Member("lokesh@mail.com", "Lokesh");
      Set<com.howtodoinjava.hibernate.equality.lombok.Member> members = new HashSet<>();
      members.add(lokesh);
      lombokEmf.runInTransaction(em -> em.persist(lokesh));
      assertFalse(members.contains(lokesh));
    }

    @Test
    void hashCodeChangesWhenTheNameChanges() {
      var alex = new com.howtodoinjava.hibernate.equality.lombok.Member("alex@mail.com", "Alex");
      Set<com.howtodoinjava.hibernate.equality.lombok.Member> members = new HashSet<>();
      members.add(alex);
      alex.setName("Alexander");
      assertFalse(members.contains(alex));
    }

    @Test
    void bidirectionalAssociationOverflowsTheStack() {
      var lokesh = new com.howtodoinjava.hibernate.equality.lombok.Member("lokesh@mail.com", "Lokesh");
      lokesh.addVisit(new com.howtodoinjava.hibernate.equality.lombok.Visit("Monday"));
      assertThrows(StackOverflowError.class, lokesh::hashCode);
      assertThrows(StackOverflowError.class, lokesh::toString);
    }

    @Test
    void toStringOnADetachedMemberLoadsTheLazyVisits() {
      var maria = new com.howtodoinjava.hibernate.equality.lombok.Member("maria@mail.com", "Maria");
      lombokEmf.runInTransaction(em -> em.persist(maria));
      var detached = lombokEmf.callInTransaction(em ->
          em.find(com.howtodoinjava.hibernate.equality.lombok.Member.class, maria.getId()));
      LazyInitializationException e = assertThrows(LazyInitializationException.class, detached::toString);
      assertTrue(e.getMessage().startsWith("Cannot lazily initialize collection of role"));
    }
  }

  /** Lombok with @EqualsAndHashCode(onlyExplicitlyIncluded = true). */
  @Nested
  class LombokFixed {

    @Test
    void equalsUsesTheEmailOnly() {
      var lokesh = new com.howtodoinjava.hibernate.equality.lombokfixed.Member("lokesh@mail.com", "Lokesh");
      lokesh.addVisit(new com.howtodoinjava.hibernate.equality.lombokfixed.Visit("Monday"));
      Set<com.howtodoinjava.hibernate.equality.lombokfixed.Member> members = new HashSet<>();
      members.add(lokesh);
      lombokFixedEmf.runInTransaction(em -> {
        em.persist(lokesh);
        em.persist(lokesh.getVisits().get(0));
      });
      lokesh.setName("Lokesh G");
      assertTrue(members.contains(lokesh));

      var detached = lombokFixedEmf.callInTransaction(em ->
          em.find(com.howtodoinjava.hibernate.equality.lombokfixed.Member.class, lokesh.getId()));
      assertTrue(detached.equals(new com.howtodoinjava.hibernate.equality.lombokfixed.Member("lokesh@mail.com", "Lokesh")));
      assertEquals("Member(email=lokesh@mail.com, name=Lokesh)", detached.toString());
    }

    @Test
    void lazyManyToOneReturnsAProxy() {
      var maria = new com.howtodoinjava.hibernate.equality.lombokfixed.Member("maria@mail.com", "Maria");
      var visit = new com.howtodoinjava.hibernate.equality.lombokfixed.Visit("Friday");
      maria.addVisit(visit);
      lombokFixedEmf.runInTransaction(em -> {
        em.persist(maria);
        em.persist(visit);
      });
      lombokFixedEmf.runInTransaction(em -> {
        var loadedVisit = em.find(com.howtodoinjava.hibernate.equality.lombokfixed.Visit.class, visit.getId());
        var member = loadedVisit.getMember();
        assertFalse(Hibernate.isInitialized(member));
        assertNotEquals(com.howtodoinjava.hibernate.equality.lombokfixed.Member.class, member.getClass());
      });
    }
  }
}
