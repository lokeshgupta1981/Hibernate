package com.howtodoinjava.hibernate.manytomany;

import com.howtodoinjava.hibernate.manytomany.entry.PlaylistItem;
import com.howtodoinjava.hibernate.manytomany.entry.PlaylistSong;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.util.List;

public class ManyToManyDemo {

  public static void main(String[] args) {
    setMapping();
    listMapping();
    joinEntity();
  }

  /** @ManyToMany with a Set, @JoinTable on the owning side and mappedBy on the inverse side. */
  static void setMapping() {
    try (EntityManagerFactory emf = Database.create("music", true, Playlist.class, Song.class)) {

      step("1. Save two playlists that share the song 'Sunrise'");
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
      Long runningId = ids[0];
      Long focusId = ids[1];
      Long sunriseId = ids[2];
      counts(emf, "playlist_song");

      step("2. Read the songs of a playlist (LAZY: a second select)");
      emf.runInTransaction(em -> {
        Playlist running = em.find(Playlist.class, runningId);
        System.out.println("songs = " + running.getSongs().stream().map(Song::getTitle).sorted().toList());
      });

      step("2b. Load a playlist with its songs in one query (join fetch)");
      Playlist fetched = emf.callInTransaction(em -> em.createQuery(
              "select p from Playlist p join fetch p.songs where p.id = :id", Playlist.class)
          .setParameter("id", runningId)
          .getSingleResult());
      System.out.println("songs = " + fetched.getSongs().stream().map(Song::getTitle).sorted().toList());

      step("3. Query the playlists that contain 'Sunrise'");
      List<String> names = emf.callInTransaction(em -> em.createQuery(
              "select p.name from Playlist p join p.songs s where s.title = :title order by p.name",
              String.class)
          .setParameter("title", "Sunrise")
          .getResultList());
      System.out.println("playlists = " + names);

      step("4. Remove 'Riverside' from 'Running' (Set: one delete)");
      SqlLog.clear();
      emf.runInTransaction(em -> {
        Playlist running = em.find(Playlist.class, runningId);
        running.removeSong(running.findSong("Riverside"));
      });
      SqlLog.writes().forEach(s -> System.out.println("  write: " + s));
      counts(emf, "playlist_song");

      step("5. Add a link from the inverse side only (not saved)");
      emf.runInTransaction(em -> {
        Song sunrise = em.find(Song.class, sunriseId);
        Playlist running = em.find(Playlist.class, runningId);
        sunrise.getPlaylists().add(running);
        Song nightDrive = em.createQuery("from Song where title = 'Night Drive'", Song.class).getSingleResult();
        nightDrive.getPlaylists().add(running);
      });
      counts(emf, "playlist_song");

      step("6. Delete 'Sunrise' while two playlists still use it");
      try {
        emf.runInTransaction(em -> em.remove(em.find(Song.class, sunriseId)));
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + rootMessage(e));
      }
      counts(emf, "playlist_song");

      step("7. Unlink 'Sunrise' from every playlist, then delete it");
      emf.runInTransaction(em -> {
        Song sunrise = em.find(Song.class, sunriseId);
        List.copyOf(sunrise.getPlaylists()).forEach(p -> p.removeSong(sunrise));
        em.remove(sunrise);
      });
      counts(emf, "playlist_song");

      step("8. Delete the 'Focus' playlist (owning side)");
      emf.runInTransaction(em -> em.remove(em.find(Playlist.class, focusId)));
      counts(emf, "playlist_song");
    }
  }

  /** The same association as a List with CascadeType.ALL: the mapping to avoid. */
  static void listMapping() {
    try (EntityManagerFactory emf = Database.create("music_bag", true,
        com.howtodoinjava.hibernate.manytomany.bag.Playlist.class,
        com.howtodoinjava.hibernate.manytomany.bag.Song.class)) {

      step("9. List mapping: save 'Running' with three songs and 'Focus' with 'Sunrise'");
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
      counts(emf, "Playlist_Song");

      step("10. List mapping: remove 'Riverside' from 'Running' (bag: delete all, insert the rest)");
      SqlLog.clear();
      emf.runInTransaction(em -> {
        var running = em.find(com.howtodoinjava.hibernate.manytomany.bag.Playlist.class, ids[0]);
        running.removeSong(running.findSong("Riverside"));
      });
      SqlLog.writes().forEach(s -> System.out.println("  write: " + s));
      counts(emf, "Playlist_Song");

      step("11. CascadeType.ALL: delete 'Running' while 'Focus' still has 'Sunrise'");
      SqlLog.clear();
      try {
        emf.runInTransaction(em -> em.remove(
            em.find(com.howtodoinjava.hibernate.manytomany.bag.Playlist.class, ids[0])));
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + rootMessage(e));
      }
      SqlLog.writes().forEach(s -> System.out.println("  write: " + s));
      counts(emf, "Playlist_Song");
    }
  }

  /** Join entity with extra columns. */
  static void joinEntity() {
    try (EntityManagerFactory emf = Database.create("music_entry", true,
        com.howtodoinjava.hibernate.manytomany.entry.Playlist.class,
        com.howtodoinjava.hibernate.manytomany.entry.Song.class,
        PlaylistSong.class, PlaylistItem.class)) {

      step("12. Join entity: add three songs with a position and a date");
      Long runningId = emf.callInTransaction(em -> {
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
      counts(emf, "playlist_song");

      step("13. Join entity: read the songs in order with their dates");
      emf.runInTransaction(em -> em.find(com.howtodoinjava.hibernate.manytomany.entry.Playlist.class, runningId)
          .getEntries()
          .forEach(e -> System.out.println(e.getPosition() + ". " + e.getSong() + " (" + e.getAddedOn() + ")")));

      step("14. Join entity: songs added to 'Running' after 2026-09-15");
      List<String> recent = emf.callInTransaction(em -> em.createQuery("""
              select ps.song.title from PlaylistSong ps
              where ps.playlist.name = :name and ps.addedOn > :date""", String.class)
          .setParameter("name", "Running")
          .setParameter("date", LocalDate.of(2026, 9, 15))
          .getResultList());
      System.out.println("recent = " + recent);

      step("15. Join entity: remove 'Sunrise' from 'Running'");
      SqlLog.clear();
      emf.runInTransaction(em -> {
        var running = em.find(com.howtodoinjava.hibernate.manytomany.entry.Playlist.class, runningId);
        var sunrise = em.createQuery("from Song where title = 'Sunrise'",
            com.howtodoinjava.hibernate.manytomany.entry.Song.class).getSingleResult();
        running.removeSong(sunrise);
      });
      SqlLog.writes().forEach(s -> System.out.println("  write: " + s));
      counts(emf, "playlist_song");

      step("16. Join entity with its own id: the same song twice in one playlist");
      emf.runInTransaction(em -> {
        var running = em.find(com.howtodoinjava.hibernate.manytomany.entry.Playlist.class, runningId);
        var sunrise = em.createQuery("from Song where title = 'Sunrise'",
            com.howtodoinjava.hibernate.manytomany.entry.Song.class).getSingleResult();
        em.persist(new PlaylistItem(running, sunrise, 1));
        em.persist(new PlaylistItem(running, sunrise, 5));
      });
      System.out.println("playlist_item rows = " + Database.count(emf, "playlist_item"));
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
  }

  private static void counts(EntityManagerFactory emf, String joinTable) {
    System.out.println("playlists=" + Database.count(emf, "Playlist")
        + " songs=" + Database.count(emf, "Song")
        + " links=" + Database.count(emf, joinTable));
  }

  private static String rootMessage(Throwable e) {
    StringBuilder chain = new StringBuilder();
    for (Throwable t = e.getCause(); t != null; t = t.getCause()) {
      chain.append(System.lineSeparator()).append("  caused by ").append(t.getClass().getName())
          .append(": ").append(t.getMessage());
    }
    return e.getMessage() + chain;
  }
}
