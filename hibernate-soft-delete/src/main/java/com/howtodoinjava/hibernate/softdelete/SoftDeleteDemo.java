package com.howtodoinjava.hibernate.softdelete;

import jakarta.persistence.EntityManagerFactory;
import java.util.Arrays;
import java.util.List;

public class SoftDeleteDemo {

  public static void main(String[] args) {
    try (EntityManagerFactory emf = Database.create(true)) {

      step("1. Save two folders with bookmarks and tags");
      Long[] ids = emf.callInTransaction(em -> {
        BookmarkFolder work = new BookmarkFolder("Work");
        Bookmark docs = new Bookmark("Hibernate Docs", "https://hibernate.org");
        docs.getTags().add("java");
        docs.getTags().add("docs");
        work.addBookmark(docs);
        work.addBookmark(new Bookmark("H2 Database", "https://h2database.com"));
        em.persist(work);

        BookmarkFolder travel = new BookmarkFolder("Travel");
        travel.addBookmark(new Bookmark("Train Times", "https://trains.example.com"));
        travel.addBookmark(new Bookmark("Hotels", "https://hotels.example.com"));
        em.persist(travel);
        return new Long[] {docs.getId(), work.getId(), travel.getId()};
      });
      Long docsId = ids[0];
      Long workId = ids[1];
      Long travelId = ids[2];

      step("2. Remove the 'H2 Database' bookmark");
      Long h2Id = emf.callInTransaction(em -> em
          .createQuery("select b.id from Bookmark b where b.title = 'H2 Database'", Long.class)
          .getSingleResult());
      emf.runInTransaction(em -> em.remove(em.find(Bookmark.class, h2Id)));

      step("3. find() and a JPQL query after the remove");
      emf.runInTransaction(em -> {
        System.out.println("find: " + em.find(Bookmark.class, h2Id));
        System.out.println("query: " + em.createQuery("select b from Bookmark b order by b.title", Bookmark.class)
            .getResultList());
      });

      step("4. Native SQL sees every row");
      print(Database.nativeRows(emf, "select title, deleted from Bookmark order by title"));

      step("5. Restore the bookmark with a native update");
      emf.runInTransaction(em -> em
          .createNativeQuery("update Bookmark set deleted = false where id = ?")
          .setParameter(1, h2Id)
          .executeUpdate());
      emf.runInTransaction(em -> System.out.println("find: " + em.find(Bookmark.class, h2Id)));

      step("6. Remove a tag from the element collection");
      emf.runInTransaction(em -> em.find(Bookmark.class, docsId).getTags().remove("docs"));
      print(Database.nativeRows(emf, "select tag, deleted from bookmark_tag order by tag"));
      emf.runInTransaction(em -> System.out.println("tags: " + em.find(Bookmark.class, docsId).getTags()));

      step("7. Remove the Travel folder (TIMESTAMP strategy, cascade to bookmarks)");
      emf.runInTransaction(em -> em.remove(em.find(BookmarkFolder.class, travelId)));
      print(Database.nativeRows(emf, "select name, deleted is not null from BookmarkFolder order by name"));
      print(Database.nativeRows(emf, "select title, deleted from Bookmark order by title"));

      step("8. JPQL bulk delete");
      emf.runInTransaction(em -> System.out.println("rows: " + em
          .createQuery("delete from Bookmark where url like '%hibernate%'")
          .executeUpdate()));
      print(Database.nativeRows(emf, "select title, deleted from Bookmark order by title"));

      step("9. Join to a soft-deleted entity");
      emf.runInTransaction(em -> System.out.println("joined: " + em
          .createQuery("select b.title, f.name from Bookmark b join b.folder f", Object[].class)
          .getResultList().stream().map(Arrays::toString).toList()));

      step("10. find() a bookmark: the LAZY folder is loaded too");
      Long h2 = h2Id;
      emf.runInTransaction(em -> System.out.println("folder: " + em.find(Bookmark.class, h2).getFolder().getName()));

      step("11. Restore the Travel folder (TIMESTAMP strategy)");
      emf.runInTransaction(em -> em
          .createNativeQuery("update BookmarkFolder set deleted = null where id = ?")
          .setParameter(1, travelId)
          .executeUpdate());
      emf.runInTransaction(em -> System.out.println("folder: " + em.find(BookmarkFolder.class, travelId)
          + ", bookmarks: " + em.find(BookmarkFolder.class, travelId).getBookmarks()));

      step("12. Re-add a removed tag (Set element collection)");
      try {
        emf.runInTransaction(em -> em.find(Bookmark.class, h2).getTags().add("db"));
        emf.runInTransaction(em -> em.find(Bookmark.class, h2).getTags().remove("db"));
        emf.runInTransaction(em -> em.find(Bookmark.class, h2).getTags().add("db"));
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }
    }

    strategies();
    uniqueNames();
    mappingErrors();
  }

  static void strategies() {
    step("13. strategy = ACTIVE");
    try (EntityManagerFactory emf = Database.create("active", true, ActiveBookmark.class)) {
      Long id = emf.callInTransaction(em -> {
        ActiveBookmark hotels = new ActiveBookmark("Hotels", "https://hotels.example.com");
        em.persist(hotels);
        return hotels.getId();
      });
      emf.runInTransaction(em -> em.remove(em.find(ActiveBookmark.class, id)));
      print(Database.nativeRows(emf, "select title, active from Bookmark"));
    }

    step("14. columnName = \"removed\", converter = YesNoConverter.class");
    try (EntityManagerFactory emf = Database.create("yesno", true, YesNoBookmark.class)) {
      Long id = emf.callInTransaction(em -> {
        YesNoBookmark hotels = new YesNoBookmark("Hotels", "https://hotels.example.com");
        em.persist(hotels);
        return hotels.getId();
      });
      emf.runInTransaction(em -> em.remove(em.find(YesNoBookmark.class, id)));
      print(Database.nativeRows(emf, "select title, removed from Bookmark"));
    }

    step("15. The older way: @SQLDelete + @SQLRestriction");
    try (EntityManagerFactory emf = Database.create("legacy", true, LegacyBookmark.class)) {
      Long[] ids = emf.callInTransaction(em -> {
        LegacyBookmark hotels = new LegacyBookmark("Hotels", "https://hotels.example.com");
        LegacyBookmark trains = new LegacyBookmark("Train Times", "https://trains.example.com");
        em.persist(hotels);
        em.persist(trains);
        return new Long[] {hotels.getId(), trains.getId()};
      });
      emf.runInTransaction(em -> em.remove(em.find(LegacyBookmark.class, ids[0])));
      emf.runInTransaction(em -> System.out.println("query: "
          + em.createQuery("select b from Bookmark b", LegacyBookmark.class).getResultList()));
      emf.runInTransaction(em -> System.out.println("rows: "
          + em.createQuery("delete from Bookmark where title = 'Train Times'").executeUpdate()));
      print(Database.nativeRows(emf, "select title, deleted from Bookmark"));
    }
  }

  static void uniqueNames() {
    step("16. Reuse the name of a deleted folder (@Column(unique = true))");
    try (EntityManagerFactory emf = Database.create("unique", true, UniqueNameFolder.class)) {
      Long id = emf.callInTransaction(em -> {
        UniqueNameFolder travel = new UniqueNameFolder("Travel");
        em.persist(travel);
        return travel.getId();
      });
      emf.runInTransaction(em -> em.remove(em.find(UniqueNameFolder.class, id)));
      try {
        emf.runInTransaction(em -> em.persist(new UniqueNameFolder("Travel")));
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }
    }

    step("17. Unique among active folders: unique nulls not distinct (name, deleted)");
    try (EntityManagerFactory emf = Database.create("unique-fixed", true, BookmarkFolder.class, Bookmark.class)) {
      emf.runInTransaction(em -> em.createNativeQuery(UNIQUE_ACTIVE_NAME).executeUpdate());
      Long id = emf.callInTransaction(em -> {
        BookmarkFolder travel = new BookmarkFolder("Travel");
        em.persist(travel);
        return travel.getId();
      });
      emf.runInTransaction(em -> em.remove(em.find(BookmarkFolder.class, id)));
      emf.runInTransaction(em -> em.persist(new BookmarkFolder("Travel")));
      try {
        emf.runInTransaction(em -> em.persist(new BookmarkFolder("Travel")));
      } catch (RuntimeException e) {
        System.out.println(e.getClass().getName() + ": " + e.getMessage());
      }
      print(Database.nativeRows(emf, "select name, deleted is not null from BookmarkFolder order by id"));
    }
  }

  static final String UNIQUE_ACTIVE_NAME =
      "alter table BookmarkFolder add constraint uk_folder_name unique nulls not distinct (name, deleted)";

  static void mappingErrors() {
    step("18. @SoftDelete on @OneToMany");
    try (EntityManagerFactory emf = Database.create("bad1", false,
        OneToManySoftDeleteFolder.class, Bookmark.class, BookmarkFolder.class)) {
      System.out.println("started");
    } catch (RuntimeException e) {
      System.out.println(e.getClass().getName() + ": " + e.getMessage());
    }

    step("19. converter with strategy = TIMESTAMP");
    try (EntityManagerFactory emf = Database.create("bad2", false, TimestampConverterBookmark.class)) {
      System.out.println("started");
    } catch (RuntimeException e) {
      System.out.println(e.getClass().getName() + ": " + e.getMessage());
    }
  }

  static void print(List<?> rows) {
    System.out.println(rows.stream()
        .map(r -> r instanceof Object[] a ? Arrays.toString(a) : String.valueOf(r))
        .toList());
  }

  static void step(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
  }
}
