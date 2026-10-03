package com.howtodoinjava.hibernate.cascade;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.RollbackException;
import java.util.Arrays;
import java.util.List;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.annotations.Cascade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CascadeTypesTest {

  private EntityManagerFactory emf;

  @BeforeEach
  void clearLog() {
    SqlLog.clear();
  }

  @AfterEach
  void close() {
    if (emf != null) {
      emf.close();
    }
  }

  private EntityManagerFactory open(Class<?>... entities) {
    emf = Database.create("test" + System.nanoTime(), false, entities);
    SqlLog.clear();
    return emf;
  }

  private long count(String entity) {
    return Database.count(emf, entity);
  }

  private List<String> photoTitles() {
    return emf.callInTransaction(em -> em.createQuery(
        "select title from Photo order by title", String.class).getResultList());
  }

  private String albumTitle() {
    return emf.callInTransaction(em -> em.createQuery(
        "select title from Album", String.class).getSingleResult());
  }

  // ------------------------------------------------------------ defaults and enums

  @Test
  void noCascadeByDefault() throws Exception {
    Object defaultValue = OneToMany.class.getMethod("cascade").getDefaultValue();
    assertEquals(0, ((jakarta.persistence.CascadeType[]) defaultValue).length);
  }

  @Test
  void jpaDefinesSixCascadeTypes() {
    assertEquals("[ALL, PERSIST, MERGE, REMOVE, REFRESH, DETACH]",
        Arrays.toString(jakarta.persistence.CascadeType.values()));
  }

  @Test
  void hibernateCascadeTypesIn74() throws Exception {
    assertEquals("[ALL, PERSIST, MERGE, REMOVE, REFRESH, DETACH, LOCK, REPLICATE, DELETE_ORPHAN]",
        Arrays.toString(org.hibernate.annotations.CascadeType.values()));
    Deprecated cascade = Cascade.class.getAnnotation(Deprecated.class);
    assertTrue(cascade.forRemoval());
    assertEquals("7", cascade.since());
    assertTrue(org.hibernate.annotations.CascadeType.class
        .getField("REPLICATE").isAnnotationPresent(Deprecated.class));
  }

  // ------------------------------------------------------------ PERSIST

  private Long persistGoa() {
    return emf.callInTransaction(em -> {
      var goa = new com.howtodoinjava.hibernate.cascade.persist.Album("Goa Trip");
      goa.addPhoto(new com.howtodoinjava.hibernate.cascade.persist.Photo("Beach"));
      goa.addPhoto(new com.howtodoinjava.hibernate.cascade.persist.Photo("Sunset"));
      em.persist(goa);
      return goa.getId();
    });
  }

  @Test
  void persistCascadesToNewPhotos() {
    open(com.howtodoinjava.hibernate.cascade.persist.Album.class,
        com.howtodoinjava.hibernate.cascade.persist.Photo.class);
    persistGoa();
    assertEquals(List.of("insert into Album (title,id) values (?,?)",
        "insert into Photo (album_id,title,id) values (?,?,?)",
        "insert into Photo (album_id,title,id) values (?,?,?)"), SqlLog.starting("insert"));
    assertEquals(1, count("Album"));
    assertEquals(List.of("Beach", "Sunset"), photoTitles());
  }

  @Test
  void persistCascadesAtFlushToPhotoAddedToManagedAlbum() {
    open(com.howtodoinjava.hibernate.cascade.persist.Album.class,
        com.howtodoinjava.hibernate.cascade.persist.Photo.class);
    Long id = persistGoa();
    SqlLog.clear();
    emf.runInTransaction(em -> em.find(com.howtodoinjava.hibernate.cascade.persist.Album.class, id)
        .addPhoto(new com.howtodoinjava.hibernate.cascade.persist.Photo("Dinner")));
    assertEquals(List.of("insert into Photo (album_id,title,id) values (?,?,?)"), SqlLog.starting("insert"));
    assertEquals(3, count("Photo"));
  }

  @Test
  void persistFailsForDetachedPhoto() {
    open(com.howtodoinjava.hibernate.cascade.persist.Album.class,
        com.howtodoinjava.hibernate.cascade.persist.Photo.class);
    persistGoa();
    var beach = emf.callInTransaction(em -> em.createQuery("from Photo where title = 'Beach'",
        com.howtodoinjava.hibernate.cascade.persist.Photo.class).getSingleResult());
    EntityExistsException e = assertThrows(EntityExistsException.class, () ->
        emf.runInTransaction(em -> {
          var best = new com.howtodoinjava.hibernate.cascade.persist.Album("Best Of");
          best.addPhoto(beach);
          em.persist(best);
        }));
    assertEquals("Detached entity passed to persist: com.howtodoinjava.hibernate.cascade.persist.Photo",
        e.getMessage());
    assertEquals(1, count("Album"));
  }

  @Test
  void photoAddedOnlyToTheListIsSavedWithoutAlbum() {
    open(com.howtodoinjava.hibernate.cascade.persist.Album.class,
        com.howtodoinjava.hibernate.cascade.persist.Photo.class);
    emf.runInTransaction(em -> {
      var goa = new com.howtodoinjava.hibernate.cascade.persist.Album("Goa Trip");
      goa.getPhotos().add(new com.howtodoinjava.hibernate.cascade.persist.Photo("Beach"));
      em.persist(goa);
    });
    assertEquals(1, count("Photo"));
    assertEquals(Long.valueOf(0), emf.<Long>callInTransaction(em -> em.createQuery(
        "select count(*) from Photo where album is not null", Long.class).getSingleResult()));
  }

  @Test
  void existingPhotoLoadedInSameTransactionMovesToNewAlbum() {
    open(com.howtodoinjava.hibernate.cascade.persist.Album.class,
        com.howtodoinjava.hibernate.cascade.persist.Photo.class);
    persistGoa();
    emf.runInTransaction(em -> {
      var beach = em.createQuery("from Photo where title = 'Beach'",
          com.howtodoinjava.hibernate.cascade.persist.Photo.class).getSingleResult();
      var best = new com.howtodoinjava.hibernate.cascade.persist.Album("Best Of");
      best.addPhoto(beach);
      em.persist(best);
    });
    assertEquals("Best Of", emf.callInTransaction(em -> em.createQuery(
        "select p.album.title from Photo p where p.title = 'Beach'", String.class).getSingleResult()));
  }

  @Test
  void mergeNewAlbumWithDetachedPhoto() {
    open(com.howtodoinjava.hibernate.cascade.persistmerge.Album.class,
        com.howtodoinjava.hibernate.cascade.persistmerge.Photo.class);
    emf.runInTransaction(em -> {
      var goa = new com.howtodoinjava.hibernate.cascade.persistmerge.Album("Goa Trip");
      goa.addPhoto(new com.howtodoinjava.hibernate.cascade.persistmerge.Photo("Beach"));
      em.persist(goa);
    });
    var beach = emf.callInTransaction(em -> em.createQuery("from Photo where title = 'Beach'",
        com.howtodoinjava.hibernate.cascade.persistmerge.Photo.class).getSingleResult());
    emf.runInTransaction(em -> {
      var best = new com.howtodoinjava.hibernate.cascade.persistmerge.Album("Best Of");
      best.addPhoto(beach);
      em.merge(best);
    });
    assertEquals(2, count("Album"));
    assertEquals(1, count("Photo"));
    assertEquals("Best Of", emf.callInTransaction(em -> em.createQuery(
        "select p.album.title from Photo p where p.title = 'Beach'", String.class).getSingleResult()));
  }

  @Test
  void withoutCascadePhotosAreSilentlyNotSaved() {
    open(com.howtodoinjava.hibernate.cascade.none.Album.class,
        com.howtodoinjava.hibernate.cascade.none.Photo.class);
    emf.runInTransaction(em -> {
      var goa = new com.howtodoinjava.hibernate.cascade.none.Album("Goa Trip");
      goa.addPhoto(new com.howtodoinjava.hibernate.cascade.none.Photo("Beach"));
      goa.addPhoto(new com.howtodoinjava.hibernate.cascade.none.Photo("Sunset"));
      em.persist(goa);
    });
    assertEquals(List.of("insert into Album (title,id) values (?,?)"), SqlLog.starting("insert"));
    assertEquals(1, count("Album"));
    assertEquals(0, count("Photo"));
  }

  @Test
  void withoutCascadePersistingPhotoWithNewAlbumFails() {
    open(com.howtodoinjava.hibernate.cascade.none.Album.class,
        com.howtodoinjava.hibernate.cascade.none.Photo.class);
    RollbackException e = assertThrows(RollbackException.class, () ->
        emf.runInTransaction(em -> {
          var goa = new com.howtodoinjava.hibernate.cascade.none.Album("Goa Trip");
          var beach = new com.howtodoinjava.hibernate.cascade.none.Photo("Beach");
          goa.addPhoto(beach);
          em.persist(beach);
        }));
    assertTrue(e.getMessage().contains("org.hibernate.TransientPropertyValueException: Persistent instance of "
        + "'com.howtodoinjava.hibernate.cascade.none.Photo' references an unsaved transient instance of "
        + "'com.howtodoinjava.hibernate.cascade.none.Album' (persist the transient instance before flushing)"));
    assertEquals(0, count("Photo"));
  }

  // ------------------------------------------------------------ MERGE

  @Test
  void mergeCascadesChangesAndNewPhotos() {
    open(com.howtodoinjava.hibernate.cascade.merge.Album.class,
        com.howtodoinjava.hibernate.cascade.merge.Photo.class);
    Long id = emf.callInTransaction(em -> {
      var goa = new com.howtodoinjava.hibernate.cascade.merge.Album("Goa Trip");
      em.persist(goa);
      var beach = new com.howtodoinjava.hibernate.cascade.merge.Photo("Beach");
      goa.addPhoto(beach);
      em.persist(beach);
      return goa.getId();
    });
    var goa = emf.callInTransaction(em -> em.createQuery("from Album a join fetch a.photos where a.id = :id",
        com.howtodoinjava.hibernate.cascade.merge.Album.class).setParameter("id", id).getSingleResult());
    goa.setTitle("Goa 2026");
    goa.findPhoto("Beach").setTitle("Beach Day");
    goa.addPhoto(new com.howtodoinjava.hibernate.cascade.merge.Photo("Boat"));
    SqlLog.clear();
    emf.runInTransaction(em -> em.merge(goa));
    List<String> writes = SqlLog.statements().stream().filter(s -> !s.startsWith("select")).toList();
    assertEquals(List.of("insert into Photo (album_id,title,id) values (?,?,?)",
        "update Photo set album_id=?,title=? where id=?",
        "update Album set title=? where id=?"), writes);
    assertEquals("Goa 2026", albumTitle());
    assertEquals(List.of("Beach Day", "Boat"), photoTitles());
  }

  @Test
  void withoutCascadeMergeIgnoresPhotoChanges() {
    open(com.howtodoinjava.hibernate.cascade.none.Album.class,
        com.howtodoinjava.hibernate.cascade.none.Photo.class);
    Long id = emf.callInTransaction(em -> {
      var goa = new com.howtodoinjava.hibernate.cascade.none.Album("Goa Trip");
      em.persist(goa);
      var beach = new com.howtodoinjava.hibernate.cascade.none.Photo("Beach");
      goa.addPhoto(beach);
      em.persist(beach);
      return goa.getId();
    });
    var goa = emf.callInTransaction(em -> em.createQuery("from Album a join fetch a.photos where a.id = :id",
        com.howtodoinjava.hibernate.cascade.none.Album.class).setParameter("id", id).getSingleResult());
    goa.setTitle("Goa 2026");
    goa.findPhoto("Beach").setTitle("Beach Day");
    goa.addPhoto(new com.howtodoinjava.hibernate.cascade.none.Photo("Boat"));
    SqlLog.clear();
    emf.runInTransaction(em -> em.merge(goa));
    List<String> writes = SqlLog.statements().stream().filter(s -> !s.startsWith("select")).toList();
    assertEquals(List.of("update Album set title=? where id=?"), writes);
    assertEquals("Goa 2026", albumTitle());
    assertEquals(List.of("Beach"), photoTitles());
  }

  // ------------------------------------------------------------ REMOVE

  @Test
  void removeCascadesToPhotos() {
    open(com.howtodoinjava.hibernate.cascade.remove.Album.class,
        com.howtodoinjava.hibernate.cascade.remove.Photo.class);
    Long id = emf.callInTransaction(em -> {
      var goa = new com.howtodoinjava.hibernate.cascade.remove.Album("Goa Trip");
      em.persist(goa);
      for (String t : new String[] {"Beach", "Sunset"}) {
        var p = new com.howtodoinjava.hibernate.cascade.remove.Photo(t);
        goa.addPhoto(p);
        em.persist(p);
      }
      return goa.getId();
    });
    SqlLog.clear();
    emf.runInTransaction(em -> em.remove(em.find(com.howtodoinjava.hibernate.cascade.remove.Album.class, id)));
    assertEquals(List.of("delete from Photo where id=?", "delete from Photo where id=?",
        "delete from Album where id=?"), SqlLog.starting("delete"));
    assertEquals(0, count("Album"));
    assertEquals(0, count("Photo"));
  }

  @Test
  void withoutCascadeRemoveFailsOnForeignKey() {
    open(com.howtodoinjava.hibernate.cascade.none.Album.class,
        com.howtodoinjava.hibernate.cascade.none.Photo.class);
    Long id = emf.callInTransaction(em -> {
      var goa = new com.howtodoinjava.hibernate.cascade.none.Album("Goa Trip");
      em.persist(goa);
      var p = new com.howtodoinjava.hibernate.cascade.none.Photo("Beach");
      goa.addPhoto(p);
      em.persist(p);
      return goa.getId();
    });
    RollbackException e = assertThrows(RollbackException.class, () -> emf.runInTransaction(em ->
        em.remove(em.find(com.howtodoinjava.hibernate.cascade.none.Album.class, id))));
    assertTrue(e.getMessage().contains("Referential integrity constraint violation"));
    assertEquals(1, count("Album"));
    assertEquals(1, count("Photo"));
  }

  @Test
  void persistAndMergeDoNotIncludeRemove() {
    open(com.howtodoinjava.hibernate.cascade.persistmerge.Album.class,
        com.howtodoinjava.hibernate.cascade.persistmerge.Photo.class);
    Long id = emf.callInTransaction(em -> {
      var goa = new com.howtodoinjava.hibernate.cascade.persistmerge.Album("Goa Trip");
      goa.addPhoto(new com.howtodoinjava.hibernate.cascade.persistmerge.Photo("Beach"));
      goa.addPhoto(new com.howtodoinjava.hibernate.cascade.persistmerge.Photo("Sunset"));
      em.persist(goa);
      return goa.getId();
    });
    assertEquals(2, count("Photo"));
    SqlLog.clear();
    RollbackException e = assertThrows(RollbackException.class, () -> emf.runInTransaction(em ->
        em.remove(em.find(com.howtodoinjava.hibernate.cascade.persistmerge.Album.class, id))));
    assertTrue(e.getMessage().contains("Referential integrity constraint violation"));
    assertEquals(List.of("delete from Album where id=?"), SqlLog.starting("delete"));
    assertEquals(1, count("Album"));
  }

  // ------------------------------------------------------------ REFRESH

  @Test
  void refreshCascadesToPhotos() {
    open(com.howtodoinjava.hibernate.cascade.refresh.Album.class,
        com.howtodoinjava.hibernate.cascade.refresh.Photo.class);
    Long id = emf.callInTransaction(em -> {
      var goa = new com.howtodoinjava.hibernate.cascade.refresh.Album("Goa Trip");
      em.persist(goa);
      var p = new com.howtodoinjava.hibernate.cascade.refresh.Photo("Beach");
      goa.addPhoto(p);
      em.persist(p);
      return goa.getId();
    });
    String[] seen = emf.callInTransaction(em -> {
      var goa = em.find(com.howtodoinjava.hibernate.cascade.refresh.Album.class, id);
      var beach = goa.findPhoto("Beach");
      goa.setTitle("Goa 2026");
      beach.setTitle("Beach Day");
      em.refresh(goa);
      return new String[] {goa.getTitle(), beach.getTitle()};
    });
    assertArrayEquals(new String[] {"Goa Trip", "Beach"}, seen);
    assertEquals(0, SqlLog.count("update"));
  }

  @Test
  void withoutCascadeRefreshKeepsPhotoChange() {
    open(com.howtodoinjava.hibernate.cascade.none.Album.class,
        com.howtodoinjava.hibernate.cascade.none.Photo.class);
    Long id = emf.callInTransaction(em -> {
      var goa = new com.howtodoinjava.hibernate.cascade.none.Album("Goa Trip");
      em.persist(goa);
      var p = new com.howtodoinjava.hibernate.cascade.none.Photo("Beach");
      goa.addPhoto(p);
      em.persist(p);
      return goa.getId();
    });
    String[] seen = emf.callInTransaction(em -> {
      var goa = em.find(com.howtodoinjava.hibernate.cascade.none.Album.class, id);
      var beach = goa.findPhoto("Beach");
      goa.setTitle("Goa 2026");
      beach.setTitle("Beach Day");
      em.refresh(goa);
      return new String[] {goa.getTitle(), beach.getTitle()};
    });
    assertArrayEquals(new String[] {"Goa Trip", "Beach Day"}, seen);
    assertEquals(List.of("update Photo set album_id=?,title=? where id=?"), SqlLog.starting("update"));
    assertEquals(List.of("Beach Day"), photoTitles());
  }

  // ------------------------------------------------------------ DETACH

  @Test
  void detachCascadesToPhotos() {
    open(com.howtodoinjava.hibernate.cascade.detach.Album.class,
        com.howtodoinjava.hibernate.cascade.detach.Photo.class);
    Long id = emf.callInTransaction(em -> {
      var goa = new com.howtodoinjava.hibernate.cascade.detach.Album("Goa Trip");
      em.persist(goa);
      var p = new com.howtodoinjava.hibernate.cascade.detach.Photo("Beach");
      goa.addPhoto(p);
      em.persist(p);
      return goa.getId();
    });
    boolean managed = emf.callInTransaction(em -> {
      var goa = em.find(com.howtodoinjava.hibernate.cascade.detach.Album.class, id);
      var beach = goa.findPhoto("Beach");
      em.detach(goa);
      beach.setTitle("Beach Day");
      return em.contains(beach);
    });
    assertFalse(managed);
    assertEquals(0, SqlLog.count("update"));
    assertEquals(List.of("Beach"), photoTitles());
  }

  @Test
  void withoutCascadeDetachLeavesPhotosManaged() {
    open(com.howtodoinjava.hibernate.cascade.none.Album.class,
        com.howtodoinjava.hibernate.cascade.none.Photo.class);
    Long id = emf.callInTransaction(em -> {
      var goa = new com.howtodoinjava.hibernate.cascade.none.Album("Goa Trip");
      em.persist(goa);
      var p = new com.howtodoinjava.hibernate.cascade.none.Photo("Beach");
      goa.addPhoto(p);
      em.persist(p);
      return goa.getId();
    });
    boolean managed = emf.callInTransaction(em -> {
      var goa = em.find(com.howtodoinjava.hibernate.cascade.none.Album.class, id);
      var beach = goa.findPhoto("Beach");
      em.detach(goa);
      beach.setTitle("Beach Day");
      return em.contains(beach);
    });
    assertTrue(managed);
    assertEquals(List.of("Beach Day"), photoTitles());
  }

  // ------------------------------------------------------------ ALL

  @Test
  void allCascadesPersistAndRemoveButKeepsRemovedPhoto() {
    open(com.howtodoinjava.hibernate.cascade.all.Album.class,
        com.howtodoinjava.hibernate.cascade.all.Photo.class);
    Long id = emf.callInTransaction(em -> {
      var goa = new com.howtodoinjava.hibernate.cascade.all.Album("Goa Trip");
      goa.addPhoto(new com.howtodoinjava.hibernate.cascade.all.Photo("Beach"));
      goa.addPhoto(new com.howtodoinjava.hibernate.cascade.all.Photo("Sunset"));
      em.persist(goa);
      return goa.getId();
    });
    assertEquals(2, count("Photo"));

    SqlLog.clear();
    emf.runInTransaction(em -> {
      var goa = em.find(com.howtodoinjava.hibernate.cascade.all.Album.class, id);
      goa.removePhoto(goa.findPhoto("Sunset"));
    });
    assertEquals(List.of("update Photo set album_id=?,title=? where id=?"), SqlLog.starting("update"));
    assertEquals(0, SqlLog.count("delete"));
    assertEquals(2, count("Photo"));

    emf.runInTransaction(em -> em.remove(em.find(com.howtodoinjava.hibernate.cascade.all.Album.class, id)));
    assertEquals(0, count("Album"));
    assertEquals(List.of("Sunset"), photoTitles());
  }

  @Test
  void allCascadesMergeRefreshAndDetach() {
    open(com.howtodoinjava.hibernate.cascade.all.Album.class,
        com.howtodoinjava.hibernate.cascade.all.Photo.class);
    Long id = emf.callInTransaction(em -> {
      var goa = new com.howtodoinjava.hibernate.cascade.all.Album("Goa Trip");
      goa.addPhoto(new com.howtodoinjava.hibernate.cascade.all.Photo("Beach"));
      em.persist(goa);
      return goa.getId();
    });

    // merge
    var detached = emf.callInTransaction(em -> em.createQuery("from Album a join fetch a.photos where a.id = :id",
        com.howtodoinjava.hibernate.cascade.all.Album.class).setParameter("id", id).getSingleResult());
    detached.findPhoto("Beach").setTitle("Beach Day");
    SqlLog.clear();
    emf.runInTransaction(em -> em.merge(detached));
    assertEquals(List.of("update Photo set album_id=?,title=? where id=?"), SqlLog.starting("update"));
    assertEquals(List.of("Beach Day"), photoTitles());

    // refresh
    String refreshed = emf.callInTransaction(em -> {
      var goa = em.find(com.howtodoinjava.hibernate.cascade.all.Album.class, id);
      var beach = goa.findPhoto("Beach Day");
      beach.setTitle("Changed");
      em.refresh(goa);
      return beach.getTitle();
    });
    assertEquals("Beach Day", refreshed);

    // detach
    boolean managed = emf.callInTransaction(em -> {
      var goa = em.find(com.howtodoinjava.hibernate.cascade.all.Album.class, id);
      var beach = goa.findPhoto("Beach Day");
      em.detach(goa);
      return em.contains(beach);
    });
    assertFalse(managed);
  }

  // ------------------------------------------------------------ Hibernate LOCK

  private Long persistLockAlbum() {
    return emf.callInTransaction(em -> {
      var goa = new com.howtodoinjava.hibernate.cascade.lock.Album("Goa Trip");
      goa.addPhoto(new com.howtodoinjava.hibernate.cascade.lock.Photo("Beach"));
      goa.addPhoto(new com.howtodoinjava.hibernate.cascade.lock.Photo("Sunset"));
      em.persist(goa);
      return goa.getId();
    });
  }

  @Test
  void lockCascadeDoesNotLockPhotos() {
    open(com.howtodoinjava.hibernate.cascade.lock.Album.class,
        com.howtodoinjava.hibernate.cascade.lock.Photo.class);
    Long id = persistLockAlbum();
    SqlLog.clear();
    emf.runInTransaction(em -> {
      var goa = em.find(com.howtodoinjava.hibernate.cascade.lock.Album.class, id);
      goa.getPhotos().size();
      em.lock(goa, LockModeType.PESSIMISTIC_WRITE);
    });
    List<String> forUpdate = SqlLog.statements().stream().filter(s -> s.endsWith("for update")).toList();
    assertEquals(List.of("select a1_0.id from Album a1_0 where a1_0.id=? for update"), forUpdate);
  }

  @Test
  void extendedScopeLocksPhotoRows() {
    open(com.howtodoinjava.hibernate.cascade.lock.Album.class,
        com.howtodoinjava.hibernate.cascade.lock.Photo.class);
    Long id = persistLockAlbum();
    SqlLog.clear();
    emf.runInTransaction(em -> {
      var goa = em.find(com.howtodoinjava.hibernate.cascade.lock.Album.class, id);
      goa.getPhotos().size();
      em.lock(goa, LockModeType.PESSIMISTIC_WRITE, PessimisticLockScope.EXTENDED);
    });
    List<String> forUpdate = SqlLog.statements().stream().filter(s -> s.endsWith("for update")).toList();
    assertEquals(List.of("select a1_0.id from Album a1_0 where a1_0.id=? for update",
        "select tbl.album_id from Photo tbl where tbl.album_id=? for update"), forUpdate);
  }

  @Test
  void extendedScopeWorksWithoutLockCascade() {
    open(com.howtodoinjava.hibernate.cascade.none.Album.class,
        com.howtodoinjava.hibernate.cascade.none.Photo.class);
    Long id = emf.callInTransaction(em -> {
      var goa = new com.howtodoinjava.hibernate.cascade.none.Album("Goa Trip");
      em.persist(goa);
      var p = new com.howtodoinjava.hibernate.cascade.none.Photo("Beach");
      goa.addPhoto(p);
      em.persist(p);
      return goa.getId();
    });
    SqlLog.clear();
    emf.runInTransaction(em -> em.lock(em.find(com.howtodoinjava.hibernate.cascade.none.Album.class, id),
        LockModeType.PESSIMISTIC_WRITE, PessimisticLockScope.EXTENDED));
    assertEquals(2, SqlLog.statements().stream().filter(s -> s.endsWith("for update")).count());
  }

  @Test
  void sessionLockNoLongerReattachesDetachedEntity() {
    open(com.howtodoinjava.hibernate.cascade.lock.Album.class,
        com.howtodoinjava.hibernate.cascade.lock.Photo.class);
    Long id = persistLockAlbum();
    var detached = emf.callInTransaction(em -> em.find(com.howtodoinjava.hibernate.cascade.lock.Album.class, id));
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
        emf.runInTransaction(em -> em.unwrap(Session.class).lock(detached, LockMode.NONE)));
    assertEquals("org.hibernate.DetachedObjectException: Given entity is not associated with the persistence context",
        e.getMessage());
  }

  // ------------------------------------------------------------ @OnDelete

  @Test
  void onDeleteLetsTheDatabaseDeletePhotos() {
    open(com.howtodoinjava.hibernate.cascade.ondelete.Album.class,
        com.howtodoinjava.hibernate.cascade.ondelete.Photo.class);
    Long id = emf.callInTransaction(em -> {
      var goa = new com.howtodoinjava.hibernate.cascade.ondelete.Album("Goa Trip");
      em.persist(goa);
      for (String t : new String[] {"Beach", "Sunset"}) {
        var p = new com.howtodoinjava.hibernate.cascade.ondelete.Photo(t);
        goa.addPhoto(p);
        em.persist(p);
      }
      return goa.getId();
    });
    SqlLog.clear();
    emf.runInTransaction(em -> em.remove(em.find(com.howtodoinjava.hibernate.cascade.ondelete.Album.class, id)));
    assertEquals(List.of("delete from Album where id=?"), SqlLog.starting("delete"));
    assertEquals(0, count("Photo"));
  }

  // ------------------------------------------------------------ Wrong: REMOVE on @ManyToOne / @ManyToMany

  private EntityManagerFactory openWrong() {
    return open(com.howtodoinjava.hibernate.cascade.wrong.Album.class,
        com.howtodoinjava.hibernate.cascade.wrong.Photo.class,
        com.howtodoinjava.hibernate.cascade.wrong.Person.class);
  }

  private Long[] seedAlbums() {
    return emf.callInTransaction(em -> {
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
  }

  @Test
  void removeOnManyToOneFailsWhenAlbumHasOtherPhotos() {
    openWrong();
    Long[] ids = seedAlbums();
    SqlLog.clear();
    RollbackException e = assertThrows(RollbackException.class, () -> emf.runInTransaction(em ->
        em.remove(em.find(com.howtodoinjava.hibernate.cascade.wrong.Photo.class, ids[0]))));
    assertTrue(e.getMessage().contains("Referential integrity constraint violation"));
    assertEquals(List.of("delete from Photo where id=?", "delete from Album where id=?"),
        SqlLog.starting("delete"));
    assertEquals(2, count("Album"));
    assertEquals(3, count("Photo"));
  }

  @Test
  void removeOnManyToOneDeletesAlbumWithItsLastPhoto() {
    openWrong();
    Long[] ids = seedAlbums();
    emf.runInTransaction(em -> em.remove(em.find(com.howtodoinjava.hibernate.cascade.wrong.Photo.class, ids[1])));
    assertEquals(List.of("Goa Trip"), emf.callInTransaction(em -> em.createQuery(
        "select title from Album", String.class).getResultList()));
  }

  private Long[] seedPeople() {
    return emf.callInTransaction(em -> {
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
  }

  @Test
  void removeOnManyToManyFailsForSharedPerson() {
    openWrong();
    Long[] ids = seedPeople();
    SqlLog.clear();
    RollbackException e = assertThrows(RollbackException.class, () -> emf.runInTransaction(em ->
        em.remove(em.find(com.howtodoinjava.hibernate.cascade.wrong.Photo.class, ids[0]))));
    assertTrue(e.getMessage().contains("Referential integrity constraint violation"));
    assertTrue(e.getMessage().contains("PHOTO_PERSON FOREIGN KEY(PERSON_ID)"));
    assertEquals(List.of("delete from photo_person where photo_id=?", "delete from Person where id=?"),
        SqlLog.starting("delete"));
    assertEquals(2, count("Person"));
  }

  @Test
  void removeOnManyToManyDeletesPersonSilently() {
    openWrong();
    Long[] ids = seedPeople();
    SqlLog.clear();
    emf.runInTransaction(em -> em.remove(em.find(com.howtodoinjava.hibernate.cascade.wrong.Photo.class, ids[1])));
    assertEquals(List.of("delete from photo_person where photo_id=?", "delete from Person where id=?",
        "delete from Photo where id=?"), SqlLog.starting("delete"));
    assertEquals(List.of("Lokesh"), emf.callInTransaction(em -> em.createQuery(
        "select name from Person", String.class).getResultList()));
  }
}
