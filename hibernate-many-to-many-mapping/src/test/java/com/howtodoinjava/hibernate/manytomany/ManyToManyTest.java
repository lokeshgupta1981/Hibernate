package com.howtodoinjava.hibernate.manytomany;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.howtodoinjava.hibernate.manytomany.entry.PlaylistItem;
import com.howtodoinjava.hibernate.manytomany.entry.PlaylistSong;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.RollbackException;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ManyToManyTest {

  /** @ManyToMany with a Set: Playlist owns playlist_song, Song is the mappedBy side. */
  @Nested
  class SetMapping {

    private final EntityManagerFactory emf = Database.create("music", false, Playlist.class, Song.class);
    private final Long runningId;
    private final Long focusId;
    private final Long sunriseId;

    SetMapping() {
      Long[] ids = emf.callInTransaction(em -> {
        Song sunrise = new Song("Sunrise");
        Song riverside = new Song("Riverside");
        Song nightDrive = new Song("Night Drive");
        em.persist(sunrise);
        em.persist(riverside);
        em.persist(nightDrive);
        Playlist running = new Playlist("Running");
        running.addSong(sunrise);
        running.addSong(riverside);
        Playlist focus = new Playlist("Focus");
        focus.addSong(sunrise);
        focus.addSong(nightDrive);
        em.persist(running);
        em.persist(focus);
        return new Long[] {running.getId(), focus.getId(), sunrise.getId()};
      });
      runningId = ids[0];
      focusId = ids[1];
      sunriseId = ids[2];
    }

    @AfterEach
    void close() {
      emf.close();
    }

    private long links() {
      return Database.count(emf, "playlist_song");
    }

    @Test
    void savingTwoPlaylistsWritesFourLinks() {
      assertEquals(2, Database.count(emf, "Playlist"));
      assertEquals(3, Database.count(emf, "Song"));
      assertEquals(4, links());
      List<String> rows = emf.callInTransaction(em -> em.createNativeQuery("""
              select p.id || ' ' || p.name || ' - ' || s.id || ' ' || s.title
              from playlist_song ps join Playlist p on p.id = ps.playlist_id join Song s on s.id = ps.song_id
              order by p.id, s.id""", String.class).getResultList());
      assertEquals(List.of("1 Running - 1 Sunrise", "1 Running - 2 Riverside",
          "2 Focus - 1 Sunrise", "2 Focus - 3 Night Drive"), rows);
    }

    @Test
    void joinTableHasCompositePrimaryKey() {
      List<String> pk = columnsOfPrimaryKey(emf, "PLAYLIST_SONG");
      assertEquals(List.of("PLAYLIST_ID", "SONG_ID"), pk);
    }

    @Test
    void bothSidesSeeTheLink() {
      emf.runInTransaction(em -> {
        assertEquals(List.of("Riverside", "Sunrise"), em.find(Playlist.class, runningId).getSongs()
            .stream().map(Song::getTitle).sorted().toList());
        assertEquals(List.of("Focus", "Running"), em.find(Song.class, sunriseId).getPlaylists()
            .stream().map(Playlist::getName).sorted().toList());
      });
    }

    @Test
    void joinQueryFindsPlaylistsOfASong() {
      List<String> names = emf.callInTransaction(em -> em.createQuery(
              "select p.name from Playlist p join p.songs s where s.title = :title order by p.name",
              String.class)
          .setParameter("title", "Sunrise")
          .getResultList());
      assertEquals(List.of("Focus", "Running"), names);
    }

    @Test
    void removingOneSongFromASetDeletesOneRow() {
      SqlLog.clear();
      emf.runInTransaction(em -> {
        Playlist running = em.find(Playlist.class, runningId);
        running.removeSong(running.findSong("Riverside"));
      });
      assertEquals(List.of("delete from playlist_song where playlist_id=? and song_id=?"), SqlLog.writes());
      assertEquals(3, links());
      assertEquals(3, Database.count(emf, "Song"));
    }

    @Test
    void songsAreLazyByDefault() {
      Playlist running = emf.callInTransaction(em -> em.find(Playlist.class, runningId));
      assertFalse(Hibernate.isInitialized(running.getSongs()));
      assertThrows(LazyInitializationException.class, () -> running.getSongs().size());
    }

    @Test
    void joinFetchLoadsTheSongsInOneQuery() {
      SqlLog.clear();
      Playlist running = emf.callInTransaction(em -> em.createQuery(
              "select p from Playlist p join fetch p.songs where p.id = :id", Playlist.class)
          .setParameter("id", runningId)
          .getSingleResult());
      assertTrue(Hibernate.isInitialized(running.getSongs()));
      assertEquals(2, running.getSongs().size());
      assertEquals(1, SqlLog.allCount());
    }

    @Test
    void changingOnlyTheInverseSideIsNotSaved() {
      emf.runInTransaction(em -> {
        Song sunrise = em.find(Song.class, sunriseId);
        Playlist running = em.find(Playlist.class, runningId);
        Playlist focus = em.find(Playlist.class, focusId);
        sunrise.getPlaylists().remove(running);
        sunrise.getPlaylists().remove(focus);
      });
      assertEquals(4, links());
    }

    @Test
    void addingASongToASetInsertsOneRow() {
      SqlLog.clear();
      emf.runInTransaction(em -> {
        Playlist running = em.find(Playlist.class, runningId);
        Song nightDrive = em.createQuery("from Song where title = 'Night Drive'", Song.class)
            .getSingleResult();
        running.addSong(nightDrive);
      });
      assertEquals(List.of("insert into playlist_song (playlist_id,song_id) values (?,?)"), SqlLog.writes());
      assertEquals(5, links());
    }

    @Test
    void addingOnlyOnTheInverseSideIsNotSaved() {
      SqlLog.clear();
      emf.runInTransaction(em -> {
        Playlist running = em.find(Playlist.class, runningId);
        Song nightDrive = em.createQuery("from Song where title = 'Night Drive'", Song.class)
            .getSingleResult();
        nightDrive.getPlaylists().add(running);
      });
      assertEquals(List.of(), SqlLog.writes());
      assertEquals(4, links());
    }

    @Test
    void cascadePersistSavesNewSongs() {
      emf.runInTransaction(em -> {
        Playlist chill = new Playlist("Chill");
        chill.addSong(new Song("Rain"));
        em.persist(chill);
      });
      assertEquals(4, Database.count(emf, "Song"));
      assertEquals(5, links());
    }

    @Test
    void addingOnlyOnTheOwningSideIsSaved() {
      emf.runInTransaction(em -> {
        Playlist running = em.find(Playlist.class, runningId);
        Song nightDrive = em.createQuery("from Song where title = 'Night Drive'", Song.class)
            .getSingleResult();
        running.getSongs().add(nightDrive);
      });
      assertEquals(5, links());
    }

    @Test
    void deletingASongThatIsStillLinkedFails() {
      RollbackException e = assertThrows(RollbackException.class, () ->
          emf.runInTransaction(em -> em.remove(em.find(Song.class, sunriseId))));
      assertInstanceOf(ConstraintViolationException.class, e.getCause());
      assertTrue(rootMessage(e).contains("Referential integrity constraint violation"));
      assertEquals(3, Database.count(emf, "Song"));
    }

    @Test
    void unlinkingASongFromEveryPlaylistLetsUsDeleteIt() {
      emf.runInTransaction(em -> {
        Song sunrise = em.find(Song.class, sunriseId);
        List.copyOf(sunrise.getPlaylists()).forEach(p -> p.removeSong(sunrise));
        em.remove(sunrise);
      });
      assertEquals(2, Database.count(emf, "Song"));
      assertEquals(2, links());
    }

    @Test
    void deletingAPlaylistDeletesItsLinksButNotTheSongs() {
      SqlLog.clear();
      emf.runInTransaction(em -> em.remove(em.find(Playlist.class, focusId)));
      assertEquals(List.of(
          "delete from playlist_song where playlist_id=?",
          "delete from Playlist where id=?"), SqlLog.writes());
      assertEquals(3, Database.count(emf, "Song"));
      assertEquals(2, links());
    }
  }

  /** The same association as a List with CascadeType.ALL and default join table names. */
  @Nested
  class ListMapping {

    private final EntityManagerFactory emf = Database.create("music_bag", false,
        com.howtodoinjava.hibernate.manytomany.bag.Playlist.class,
        com.howtodoinjava.hibernate.manytomany.bag.Song.class);
    private final Long runningId;
    private final Long focusId;

    ListMapping() {
      Long[] ids = emf.callInTransaction(em -> {
        var running = new com.howtodoinjava.hibernate.manytomany.bag.Playlist("Running");
        var sunrise = new com.howtodoinjava.hibernate.manytomany.bag.Song("Sunrise");
        running.addSong(sunrise);
        running.addSong(new com.howtodoinjava.hibernate.manytomany.bag.Song("Riverside"));
        running.addSong(new com.howtodoinjava.hibernate.manytomany.bag.Song("Night Drive"));
        var focus = new com.howtodoinjava.hibernate.manytomany.bag.Playlist("Focus");
        focus.addSong(sunrise);
        em.persist(running);
        em.persist(focus);
        return new Long[] {running.getId(), focus.getId()};
      });
      runningId = ids[0];
      focusId = ids[1];
    }

    @AfterEach
    void close() {
      emf.close();
    }

    @Test
    void defaultJoinTableNamesAndNoPrimaryKey() {
      List<String> columns = emf.callInTransaction(em -> em.createNativeQuery("""
              select column_name from information_schema.columns
              where table_name = 'PLAYLIST_SONG' order by column_name""", String.class)
          .getResultList());
      assertEquals(List.of("PLAYLISTS_ID", "SONGS_ID"), columns);
      assertEquals(List.of(), columnsOfPrimaryKey(emf, "PLAYLIST_SONG"));
    }

    @Test
    void removingOneSongFromAListDeletesAllRowsAndInsertsTheRest() {
      SqlLog.clear();
      emf.runInTransaction(em -> {
        var running = em.find(com.howtodoinjava.hibernate.manytomany.bag.Playlist.class, runningId);
        running.removeSong(running.findSong("Riverside"));
      });
      assertEquals(List.of(
          "delete from Playlist_Song where playlists_id=?",
          "insert into Playlist_Song (playlists_id,songs_id) values (?,?)",
          "insert into Playlist_Song (playlists_id,songs_id) values (?,?)"), SqlLog.writes());
      assertEquals(3, Database.count(emf, "Playlist_Song"));
    }

    @Test
    void cascadeAllFailsWhenAnotherPlaylistUsesTheSong() {
      SqlLog.clear();
      RollbackException e = assertThrows(RollbackException.class, () -> emf.runInTransaction(em ->
          em.remove(em.find(com.howtodoinjava.hibernate.manytomany.bag.Playlist.class, runningId))));
      assertInstanceOf(ConstraintViolationException.class, e.getCause());
      assertTrue(rootMessage(e).contains("Referential integrity constraint violation"));
      assertEquals(List.of(
          "delete from Playlist_Song where playlists_id=?",
          "delete from Song where id=?"), SqlLog.writes());
      assertEquals(3, Database.count(emf, "Song"));
    }

    @Test
    void cascadeAllDeletesSongsFromTheLibrary() {
      emf.runInTransaction(em -> {
        var focus = em.find(com.howtodoinjava.hibernate.manytomany.bag.Playlist.class, focusId);
        focus.removeSong(focus.findSong("Sunrise"));
      });
      emf.runInTransaction(em ->
          em.remove(em.find(com.howtodoinjava.hibernate.manytomany.bag.Playlist.class, runningId)));
      assertEquals(0, Database.count(emf, "Song"));
      assertEquals(1, Database.count(emf, "Playlist"));
    }
  }

  /** The link as an entity with extra columns. */
  @Nested
  class JoinEntity {

    private final EntityManagerFactory emf = Database.create("music_entry", false,
        com.howtodoinjava.hibernate.manytomany.entry.Playlist.class,
        com.howtodoinjava.hibernate.manytomany.entry.Song.class,
        PlaylistSong.class, PlaylistItem.class);
    private final Long runningId;

    JoinEntity() {
      runningId = emf.callInTransaction(em -> {
        var sunrise = new com.howtodoinjava.hibernate.manytomany.entry.Song("Sunrise");
        var riverside = new com.howtodoinjava.hibernate.manytomany.entry.Song("Riverside");
        var nightDrive = new com.howtodoinjava.hibernate.manytomany.entry.Song("Night Drive");
        em.persist(sunrise);
        em.persist(riverside);
        em.persist(nightDrive);
        var running = new com.howtodoinjava.hibernate.manytomany.entry.Playlist("Running");
        running.addSong(nightDrive, 1, LocalDate.of(2026, 9, 1));
        running.addSong(sunrise, 2, LocalDate.of(2026, 9, 1));
        running.addSong(riverside, 3, LocalDate.of(2026, 10, 2));
        em.persist(running);
        return running.getId();
      });
    }

    @AfterEach
    void close() {
      emf.close();
    }

    private com.howtodoinjava.hibernate.manytomany.entry.Song song(jakarta.persistence.EntityManager em,
        String title) {
      return em.createQuery("from Song where title = :t", com.howtodoinjava.hibernate.manytomany.entry.Song.class)
          .setParameter("t", title).getSingleResult();
    }

    @Test
    void joinTableHasExtraColumnsAndCompositeKey() {
      List<String> columns = emf.callInTransaction(em -> em.createNativeQuery("""
              select column_name from information_schema.columns
              where table_name = 'PLAYLIST_SONG' order by column_name""", String.class)
          .getResultList());
      assertEquals(List.of("ADDED_ON", "PLAYLIST_ID", "POSITION", "SONG_ID"), columns);
      assertEquals(List.of("PLAYLIST_ID", "SONG_ID"), columnsOfPrimaryKey(emf, "PLAYLIST_SONG"));
      assertEquals(3, Database.count(emf, "playlist_song"));
    }

    @Test
    void entriesComeBackOrderedByPosition() {
      List<String> lines = emf.callInTransaction(em -> em
          .find(com.howtodoinjava.hibernate.manytomany.entry.Playlist.class, runningId)
          .getEntries().stream()
          .map(e -> e.getPosition() + ". " + e.getSong().getTitle() + " (" + e.getAddedOn() + ")")
          .toList());
      assertEquals(List.of(
          "1. Night Drive (2026-09-01)",
          "2. Sunrise (2026-09-01)",
          "3. Riverside (2026-10-02)"), lines);
    }

    @Test
    void queryOnTheExtraColumn() {
      List<String> recent = emf.callInTransaction(em -> em.createQuery("""
              select ps.song.title from PlaylistSong ps
              where ps.playlist.name = :name and ps.addedOn > :date""", String.class)
          .setParameter("name", "Running")
          .setParameter("date", LocalDate.of(2026, 9, 15))
          .getResultList());
      assertEquals(List.of("Riverside"), recent);
    }

    @Test
    void removingASongDeletesOneJoinRow() {
      SqlLog.clear();
      emf.runInTransaction(em -> em
          .find(com.howtodoinjava.hibernate.manytomany.entry.Playlist.class, runningId)
          .removeSong(song(em, "Sunrise")));
      assertEquals(List.of("delete from playlist_song where playlist_id=? and song_id=?"), SqlLog.writes());
      assertEquals(2, Database.count(emf, "playlist_song"));
      assertEquals(3, Database.count(emf, "Song"));
    }

    @Test
    void changingThePositionUpdatesTheJoinRow() {
      SqlLog.clear();
      emf.runInTransaction(em -> em
          .find(com.howtodoinjava.hibernate.manytomany.entry.Playlist.class, runningId)
          .getEntries().getFirst().setPosition(4));
      assertEquals(List.of(
          "update playlist_song set added_on=?,position=? where playlist_id=? and song_id=?"),
          SqlLog.writes());
    }

    @Test
    void addingTheSameSongTwiceViolatesTheCompositeKey() {
      RollbackException e = assertThrows(RollbackException.class, () -> emf.runInTransaction(em -> em
          .find(com.howtodoinjava.hibernate.manytomany.entry.Playlist.class, runningId)
          .addSong(song(em, "Sunrise"), 4, LocalDate.of(2026, 10, 3))));
      assertTrue(rootMessage(e).contains("Unique index or primary key violation"));
      assertEquals(3, Database.count(emf, "playlist_song"));
    }

    @Test
    void joinEntityWithOwnIdAllowsTheSameSongTwice() {
      emf.runInTransaction(em -> {
        var running = em.find(com.howtodoinjava.hibernate.manytomany.entry.Playlist.class, runningId);
        var sunrise = song(em, "Sunrise");
        em.persist(new PlaylistItem(running, sunrise, 1));
        em.persist(new PlaylistItem(running, sunrise, 5));
      });
      assertEquals(2, Database.count(emf, "playlist_item"));
    }
  }

  static List<String> columnsOfPrimaryKey(EntityManagerFactory emf, String table) {
    return emf.callInTransaction(em -> em.createNativeQuery("""
            select k.column_name from information_schema.key_column_usage k
            join information_schema.table_constraints c on c.constraint_name = k.constraint_name
            where c.table_name = ?1 and c.constraint_type = 'PRIMARY KEY'
            order by k.column_name""", String.class)
        .setParameter(1, table)
        .getResultList());
  }

  static String rootMessage(Throwable e) {
    Throwable t = e;
    while (t.getCause() != null) {
      t = t.getCause();
    }
    return t.getMessage();
  }
}
