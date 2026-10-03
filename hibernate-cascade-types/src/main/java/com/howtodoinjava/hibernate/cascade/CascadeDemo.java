package com.howtodoinjava.hibernate.cascade;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PessimisticLockScope;
import org.hibernate.LockMode;
import org.hibernate.Session;

/** Runs every cascade case from the article and prints the SQL Hibernate sends. */
public class CascadeDemo {

  public static void main(String[] args) {
    persist();
    merge();
    remove();
    refresh();
    detach();
    all();
    lock();
    persistAndMerge();
    onDelete();
    wrongManyToOne();
    wrongManyToMany();
  }

  // ---------------------------------------------------------------- PERSIST

  static void persist() {
    try (EntityManagerFactory emf = Database.create("persist", true,
        com.howtodoinjava.hibernate.cascade.persist.Album.class,
        com.howtodoinjava.hibernate.cascade.persist.Photo.class)) {

      step("PERSIST: em.persist(album) with two new photos");
      Long albumId = emf.callInTransaction(em -> {
        var goa = new com.howtodoinjava.hibernate.cascade.persist.Album("Goa Trip");
        goa.addPhoto(new com.howtodoinjava.hibernate.cascade.persist.Photo("Beach"));
        goa.addPhoto(new com.howtodoinjava.hibernate.cascade.persist.Photo("Sunset"));
        em.persist(goa);
        return goa.getId();
      });
      counts(emf);

      step("PERSIST: add a photo to a managed album, no persist() call");
      emf.runInTransaction(em -> {
        var goa = em.find(com.howtodoinjava.hibernate.cascade.persist.Album.class, albumId);
        goa.addPhoto(new com.howtodoinjava.hibernate.cascade.persist.Photo("Dinner"));
      });
      counts(emf);

      step("PERSIST: put an existing (detached) photo into a new album");
      var beach = emf.callInTransaction(em -> em.createQuery(
          "from Photo where title = 'Beach'", com.howtodoinjava.hibernate.cascade.persist.Photo.class)
          .getSingleResult());
      try {
        emf.runInTransaction(em -> {
          var best = new com.howtodoinjava.hibernate.cascade.persist.Album("Best Of");
          best.addPhoto(beach);
          em.persist(best);
        });
      } catch (RuntimeException e) {
        error(e);
      }
    }

    try (EntityManagerFactory emf = Database.create("persistNone", true,
        com.howtodoinjava.hibernate.cascade.none.Album.class,
        com.howtodoinjava.hibernate.cascade.none.Photo.class)) {

      step("No cascade: em.persist(album) with two new photos");
      try {
        emf.runInTransaction(em -> {
          var goa = new com.howtodoinjava.hibernate.cascade.none.Album("Goa Trip");
          goa.addPhoto(new com.howtodoinjava.hibernate.cascade.none.Photo("Beach"));
          goa.addPhoto(new com.howtodoinjava.hibernate.cascade.none.Photo("Sunset"));
          em.persist(goa);
        });
      } catch (RuntimeException e) {
        error(e);
      }
      counts(emf);

      step("No cascade: em.persist(photo) whose album is new");
      try {
        emf.runInTransaction(em -> {
          var goa = new com.howtodoinjava.hibernate.cascade.none.Album("Goa Trip");
          var beach2 = new com.howtodoinjava.hibernate.cascade.none.Photo("Beach");
          goa.addPhoto(beach2);
          em.persist(beach2);
        });
      } catch (RuntimeException e) {
        error(e);
      }
      counts(emf);
    }
  }

  // ---------------------------------------------------------------- MERGE

  static void merge() {
    try (EntityManagerFactory emf = Database.create("merge", true,
        com.howtodoinjava.hibernate.cascade.merge.Album.class,
        com.howtodoinjava.hibernate.cascade.merge.Photo.class)) {

      Long albumId = emf.callInTransaction(em -> {
        var goa = new com.howtodoinjava.hibernate.cascade.merge.Album("Goa Trip");
        em.persist(goa);
        var beach = new com.howtodoinjava.hibernate.cascade.merge.Photo("Beach");
        goa.addPhoto(beach);
        em.persist(beach);
        return goa.getId();
      });

      step("MERGE: merge a detached album with a changed photo and a new photo");
      var goa = emf.callInTransaction(em -> em.createQuery(
          "from Album a join fetch a.photos where a.id = :id",
          com.howtodoinjava.hibernate.cascade.merge.Album.class)
          .setParameter("id", albumId).getSingleResult());
      goa.setTitle("Goa 2026");
      goa.findPhoto("Beach").setTitle("Beach Day");
      goa.addPhoto(new com.howtodoinjava.hibernate.cascade.merge.Photo("Boat"));
      emf.runInTransaction(em -> em.merge(goa));
      titles(emf);
    }

    try (EntityManagerFactory emf = Database.create("mergeNone", true,
        com.howtodoinjava.hibernate.cascade.none.Album.class,
        com.howtodoinjava.hibernate.cascade.none.Photo.class)) {

      Long albumId = emf.callInTransaction(em -> {
        var goa = new com.howtodoinjava.hibernate.cascade.none.Album("Goa Trip");
        em.persist(goa);
        var beach = new com.howtodoinjava.hibernate.cascade.none.Photo("Beach");
        goa.addPhoto(beach);
        em.persist(beach);
        return goa.getId();
      });

      step("No cascade: merge a detached album with a changed photo");
      var goa = emf.callInTransaction(em -> em.createQuery(
          "from Album a join fetch a.photos where a.id = :id",
          com.howtodoinjava.hibernate.cascade.none.Album.class)
          .setParameter("id", albumId).getSingleResult());
      goa.setTitle("Goa 2026");
      goa.findPhoto("Beach").setTitle("Beach Day");
      emf.runInTransaction(em -> em.merge(goa));
      titles(emf);

      step("No cascade: merge a detached album with a new photo");
      goa.addPhoto(new com.howtodoinjava.hibernate.cascade.none.Photo("Boat"));
      try {
        emf.runInTransaction(em -> em.merge(goa));
      } catch (RuntimeException e) {
        error(e);
      }
      titles(emf);
    }
  }

  // ---------------------------------------------------------------- REMOVE

  static void remove() {
    try (EntityManagerFactory emf = Database.create("remove", true,
        com.howtodoinjava.hibernate.cascade.remove.Album.class,
        com.howtodoinjava.hibernate.cascade.remove.Photo.class)) {

      Long albumId = emf.callInTransaction(em -> {
        var goa = new com.howtodoinjava.hibernate.cascade.remove.Album("Goa Trip");
        em.persist(goa);
        for (String t : new String[] {"Beach", "Sunset"}) {
          var p = new com.howtodoinjava.hibernate.cascade.remove.Photo(t);
          goa.addPhoto(p);
          em.persist(p);
        }
        return goa.getId();
      });

      step("REMOVE: em.remove(album)");
      emf.runInTransaction(em -> em.remove(
          em.find(com.howtodoinjava.hibernate.cascade.remove.Album.class, albumId)));
      counts(emf);
    }

    try (EntityManagerFactory emf = Database.create("removeNone", true,
        com.howtodoinjava.hibernate.cascade.none.Album.class,
        com.howtodoinjava.hibernate.cascade.none.Photo.class)) {

      Long albumId = emf.callInTransaction(em -> {
        var goa = new com.howtodoinjava.hibernate.cascade.none.Album("Goa Trip");
        em.persist(goa);
        var p = new com.howtodoinjava.hibernate.cascade.none.Photo("Beach");
        goa.addPhoto(p);
        em.persist(p);
        return goa.getId();
      });

      step("No cascade: em.remove(album) while photos point to it");
      try {
        emf.runInTransaction(em -> em.remove(
            em.find(com.howtodoinjava.hibernate.cascade.none.Album.class, albumId)));
      } catch (RuntimeException e) {
        error(e);
      }
      counts(emf);
    }
  }

  // ---------------------------------------------------------------- REFRESH

  static void refresh() {
    try (EntityManagerFactory emf = Database.create("refresh", true,
        com.howtodoinjava.hibernate.cascade.refresh.Album.class,
        com.howtodoinjava.hibernate.cascade.refresh.Photo.class)) {

      Long albumId = emf.callInTransaction(em -> {
        var goa = new com.howtodoinjava.hibernate.cascade.refresh.Album("Goa Trip");
        em.persist(goa);
        var p = new com.howtodoinjava.hibernate.cascade.refresh.Photo("Beach");
        goa.addPhoto(p);
        em.persist(p);
        return goa.getId();
      });

      step("REFRESH: change album and photo, then em.refresh(album)");
      emf.runInTransaction(em -> {
        var goa = em.find(com.howtodoinjava.hibernate.cascade.refresh.Album.class, albumId);
        var beach = goa.findPhoto("Beach");
        goa.setTitle("Goa 2026");
        beach.setTitle("Beach Day");
        em.refresh(goa);
        System.out.println("album=" + goa.getTitle() + " photo=" + beach.getTitle());
      });
      titles(emf);
    }

    try (EntityManagerFactory emf = Database.create("refreshNone", true,
        com.howtodoinjava.hibernate.cascade.none.Album.class,
        com.howtodoinjava.hibernate.cascade.none.Photo.class)) {

      Long albumId = emf.callInTransaction(em -> {
        var goa = new com.howtodoinjava.hibernate.cascade.none.Album("Goa Trip");
        em.persist(goa);
        var p = new com.howtodoinjava.hibernate.cascade.none.Photo("Beach");
        goa.addPhoto(p);
        em.persist(p);
        return goa.getId();
      });

      step("No cascade: change album and photo, then em.refresh(album)");
      emf.runInTransaction(em -> {
        var goa = em.find(com.howtodoinjava.hibernate.cascade.none.Album.class, albumId);
        var beach = goa.findPhoto("Beach");
        goa.setTitle("Goa 2026");
        beach.setTitle("Beach Day");
        em.refresh(goa);
        System.out.println("album=" + goa.getTitle() + " photo=" + beach.getTitle());
      });
      titles(emf);
    }
  }

  // ---------------------------------------------------------------- DETACH

  static void detach() {
    try (EntityManagerFactory emf = Database.create("detach", true,
        com.howtodoinjava.hibernate.cascade.detach.Album.class,
        com.howtodoinjava.hibernate.cascade.detach.Photo.class)) {

      Long albumId = emf.callInTransaction(em -> {
        var goa = new com.howtodoinjava.hibernate.cascade.detach.Album("Goa Trip");
        em.persist(goa);
        var p = new com.howtodoinjava.hibernate.cascade.detach.Photo("Beach");
        goa.addPhoto(p);
        em.persist(p);
        return goa.getId();
      });

      step("DETACH: em.detach(album), then change the photo");
      emf.runInTransaction(em -> {
        var goa = em.find(com.howtodoinjava.hibernate.cascade.detach.Album.class, albumId);
        var beach = goa.findPhoto("Beach");
        em.detach(goa);
        System.out.println("contains(album)=" + em.contains(goa) + " contains(photo)=" + em.contains(beach));
        beach.setTitle("Beach Day");
      });
      titles(emf);
    }

    try (EntityManagerFactory emf = Database.create("detachNone", true,
        com.howtodoinjava.hibernate.cascade.none.Album.class,
        com.howtodoinjava.hibernate.cascade.none.Photo.class)) {

      Long albumId = emf.callInTransaction(em -> {
        var goa = new com.howtodoinjava.hibernate.cascade.none.Album("Goa Trip");
        em.persist(goa);
        var p = new com.howtodoinjava.hibernate.cascade.none.Photo("Beach");
        goa.addPhoto(p);
        em.persist(p);
        return goa.getId();
      });

      step("No cascade: em.detach(album), then change the photo");
      emf.runInTransaction(em -> {
        var goa = em.find(com.howtodoinjava.hibernate.cascade.none.Album.class, albumId);
        var beach = goa.findPhoto("Beach");
        em.detach(goa);
        System.out.println("contains(album)=" + em.contains(goa) + " contains(photo)=" + em.contains(beach));
        beach.setTitle("Beach Day");
      });
      titles(emf);
    }
  }

  // ---------------------------------------------------------------- ALL and LOCK

  static void all() {
    try (EntityManagerFactory emf = Database.create("all", true,
        com.howtodoinjava.hibernate.cascade.all.Album.class,
        com.howtodoinjava.hibernate.cascade.all.Photo.class)) {

      step("ALL: em.persist(album) with two new photos");
      Long albumId = emf.callInTransaction(em -> {
        var goa = new com.howtodoinjava.hibernate.cascade.all.Album("Goa Trip");
        goa.addPhoto(new com.howtodoinjava.hibernate.cascade.all.Photo("Beach"));
        goa.addPhoto(new com.howtodoinjava.hibernate.cascade.all.Photo("Sunset"));
        em.persist(goa);
        return goa.getId();
      });
      counts(emf);

      step("ALL: remove a photo from the album (no orphanRemoval)");
      emf.runInTransaction(em -> {
        var goa = em.find(com.howtodoinjava.hibernate.cascade.all.Album.class, albumId);
        goa.removePhoto(goa.findPhoto("Sunset"));
      });
      counts(emf);

      step("ALL: em.remove(album)");
      emf.runInTransaction(em -> em.remove(
          em.find(com.howtodoinjava.hibernate.cascade.all.Album.class, albumId)));
      counts(emf);
    }
  }

  // ---------------------------------------------------------------- Hibernate @Cascade(LOCK)

  static void lock() {
    try (EntityManagerFactory emf = Database.create("lock", true,
        com.howtodoinjava.hibernate.cascade.lock.Album.class,
        com.howtodoinjava.hibernate.cascade.lock.Photo.class)) {

      Long albumId = emf.callInTransaction(em -> {
        var goa = new com.howtodoinjava.hibernate.cascade.lock.Album("Goa Trip");
        goa.addPhoto(new com.howtodoinjava.hibernate.cascade.lock.Photo("Beach"));
        goa.addPhoto(new com.howtodoinjava.hibernate.cascade.lock.Photo("Sunset"));
        em.persist(goa);
        return goa.getId();
      });

      step("@Cascade(LOCK): em.lock(album, PESSIMISTIC_WRITE) with the photos loaded");
      emf.runInTransaction(em -> {
        var goa = em.find(com.howtodoinjava.hibernate.cascade.lock.Album.class, albumId);
        goa.getPhotos().size();
        em.lock(goa, LockModeType.PESSIMISTIC_WRITE);
      });

      step("@Cascade(LOCK): the same lock with PessimisticLockScope.EXTENDED");
      emf.runInTransaction(em -> {
        var goa = em.find(com.howtodoinjava.hibernate.cascade.lock.Album.class, albumId);
        goa.getPhotos().size();
        em.lock(goa, LockModeType.PESSIMISTIC_WRITE, PessimisticLockScope.EXTENDED);
      });

      step("@Cascade(LOCK): Session.lock() on a detached album");
      var detached = emf.callInTransaction(em ->
          em.find(com.howtodoinjava.hibernate.cascade.lock.Album.class, albumId));
      try {
        emf.runInTransaction(em -> em.unwrap(Session.class).lock(detached, LockMode.NONE));
      } catch (RuntimeException e) {
        error(e);
      }
    }
  }

  // ---------------------------------------------------------------- PERSIST + MERGE

  static void persistAndMerge() {
    try (EntityManagerFactory emf = Database.create("persistMerge", true,
        com.howtodoinjava.hibernate.cascade.persistmerge.Album.class,
        com.howtodoinjava.hibernate.cascade.persistmerge.Photo.class)) {

      step("{PERSIST, MERGE}: em.persist(album) with two new photos");
      Long albumId = emf.callInTransaction(em -> {
        var goa = new com.howtodoinjava.hibernate.cascade.persistmerge.Album("Goa Trip");
        goa.addPhoto(new com.howtodoinjava.hibernate.cascade.persistmerge.Photo("Beach"));
        goa.addPhoto(new com.howtodoinjava.hibernate.cascade.persistmerge.Photo("Sunset"));
        em.persist(goa);
        return goa.getId();
      });
      counts(emf);

      step("{PERSIST, MERGE}: em.remove(album) does not reach the photos");
      try {
        emf.runInTransaction(em -> em.remove(
            em.find(com.howtodoinjava.hibernate.cascade.persistmerge.Album.class, albumId)));
      } catch (RuntimeException e) {
        error(e);
      }
      counts(emf);
    }
  }

  // ---------------------------------------------------------------- @OnDelete

  static void onDelete() {
    try (EntityManagerFactory emf = Database.create("onDelete", true,
        com.howtodoinjava.hibernate.cascade.ondelete.Album.class,
        com.howtodoinjava.hibernate.cascade.ondelete.Photo.class)) {

      Long albumId = emf.callInTransaction(em -> {
        var goa = new com.howtodoinjava.hibernate.cascade.ondelete.Album("Goa Trip");
        em.persist(goa);
        for (String t : new String[] {"Beach", "Sunset"}) {
          var p = new com.howtodoinjava.hibernate.cascade.ondelete.Photo(t);
          goa.addPhoto(p);
          em.persist(p);
        }
        return goa.getId();
      });

      step("@OnDelete(CASCADE): em.remove(album), the database deletes the photos");
      emf.runInTransaction(em -> em.remove(
          em.find(com.howtodoinjava.hibernate.cascade.ondelete.Album.class, albumId)));
      counts(emf);
    }
  }

  // ---------------------------------------------------------------- Wrong mappings

  static void wrongManyToOne() {
    try (EntityManagerFactory emf = Database.create("wrongManyToOne", true,
        com.howtodoinjava.hibernate.cascade.wrong.Album.class,
        com.howtodoinjava.hibernate.cascade.wrong.Photo.class,
        com.howtodoinjava.hibernate.cascade.wrong.Person.class)) {

      Long[] ids = emf.callInTransaction(em -> {
        var goa = new com.howtodoinjava.hibernate.cascade.wrong.Album("Goa Trip");
        em.persist(goa);
        var beach = new com.howtodoinjava.hibernate.cascade.wrong.Photo("Beach");
        var sunset = new com.howtodoinjava.hibernate.cascade.wrong.Photo("Sunset");
        goa.addPhoto(beach);
        goa.addPhoto(sunset);
        em.persist(beach);
        em.persist(sunset);
        var party = new com.howtodoinjava.hibernate.cascade.wrong.Album("Party");
        em.persist(party);
        var cake = new com.howtodoinjava.hibernate.cascade.wrong.Photo("Cake");
        party.addPhoto(cake);
        em.persist(cake);
        return new Long[] {beach.getId(), cake.getId()};
      });

      step("Wrong: @ManyToOne(cascade = REMOVE), delete one photo of a two-photo album");
      try {
        emf.runInTransaction(em -> em.remove(
            em.find(com.howtodoinjava.hibernate.cascade.wrong.Photo.class, ids[0])));
      } catch (RuntimeException e) {
        error(e);
      }
      counts3(emf);

      step("Wrong: @ManyToOne(cascade = REMOVE), delete the only photo of an album");
      emf.runInTransaction(em -> em.remove(
          em.find(com.howtodoinjava.hibernate.cascade.wrong.Photo.class, ids[1])));
      counts3(emf);
    }
  }

  static void wrongManyToMany() {
    try (EntityManagerFactory emf = Database.create("wrongManyToMany", true,
        com.howtodoinjava.hibernate.cascade.wrong.Album.class,
        com.howtodoinjava.hibernate.cascade.wrong.Photo.class,
        com.howtodoinjava.hibernate.cascade.wrong.Person.class)) {

      Long[] ids = emf.callInTransaction(em -> {
        var lokesh = new com.howtodoinjava.hibernate.cascade.wrong.Person("Lokesh");
        var alex = new com.howtodoinjava.hibernate.cascade.wrong.Person("Alex");
        em.persist(lokesh);
        em.persist(alex);
        var beach = new com.howtodoinjava.hibernate.cascade.wrong.Photo("Beach");
        beach.getPeople().add(lokesh);
        var sunset = new com.howtodoinjava.hibernate.cascade.wrong.Photo("Sunset");
        sunset.getPeople().add(lokesh);
        var cake = new com.howtodoinjava.hibernate.cascade.wrong.Photo("Cake");
        cake.getPeople().add(alex);
        em.persist(beach);
        em.persist(sunset);
        em.persist(cake);
        return new Long[] {beach.getId(), cake.getId()};
      });

      step("Wrong: @ManyToMany(cascade = REMOVE), delete a photo whose person is in another photo");
      try {
        emf.runInTransaction(em -> em.remove(
            em.find(com.howtodoinjava.hibernate.cascade.wrong.Photo.class, ids[0])));
      } catch (RuntimeException e) {
        error(e);
      }
      counts3(emf);

      step("Wrong: @ManyToMany(cascade = REMOVE), delete a photo whose person is in no other photo");
      emf.runInTransaction(em -> em.remove(
          em.find(com.howtodoinjava.hibernate.cascade.wrong.Photo.class, ids[1])));
      counts3(emf);
    }
  }

  // ---------------------------------------------------------------- helpers

  private static void step(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
  }

  private static void error(RuntimeException e) {
    System.out.println("ERROR " + e.getClass().getName() + ": " + e.getMessage());
    Throwable cause = e.getCause();
    while (cause != null) {
      System.out.println("  caused by " + cause.getClass().getName() + ": " + cause.getMessage());
      cause = cause.getCause();
    }
  }

  private static void counts(EntityManagerFactory emf) {
    System.out.println("albums=" + Database.count(emf, "Album") + " photos=" + Database.count(emf, "Photo"));
  }

  private static void counts3(EntityManagerFactory emf) {
    System.out.println("albums=" + Database.count(emf, "Album") + " photos=" + Database.count(emf, "Photo")
        + " people=" + Database.count(emf, "Person"));
  }

  private static void titles(EntityManagerFactory emf) {
    System.out.println(emf.callInTransaction(em -> em.createQuery(
        "select a.title from Album a", String.class).getResultList())
        + " " + emf.callInTransaction(em -> em.createQuery(
        "select p.title || ' -> album ' || coalesce(cast(p.album.id as String), 'null') from Photo p left join p.album order by p.title",
        String.class).getResultList()));
  }
}
