package com.howtodoinjava.hibernate.softdelete;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.RollbackException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.type.NumericBooleanConverter;
import org.hibernate.type.TrueFalseConverter;
import org.hibernate.type.YesNoConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SoftDeleteTest {

  private final List<String> sql = new CopyOnWriteArrayList<>();
  private final StatementInspector inspector = s -> {
    sql.add(s);
    return s;
  };

  private EntityManagerFactory emf;
  private Long docsId;
  private Long h2Id;
  private Long workId;
  private Long travelId;

  @BeforeEach
  void setUp() {
    emf = Database.create("test", false, inspector, BookmarkFolder.class, Bookmark.class);
    Long[] ids = emf.callInTransaction(em -> {
      BookmarkFolder work = new BookmarkFolder("Work");
      Bookmark docs = new Bookmark("Hibernate Docs", "https://hibernate.org");
      docs.getTags().add("java");
      docs.getTags().add("docs");
      work.addBookmark(docs);
      Bookmark h2 = new Bookmark("H2 Database", "https://h2database.com");
      work.addBookmark(h2);
      em.persist(work);

      BookmarkFolder travel = new BookmarkFolder("Travel");
      travel.addBookmark(new Bookmark("Train Times", "https://trains.example.com"));
      travel.addBookmark(new Bookmark("Hotels", "https://hotels.example.com"));
      em.persist(travel);
      return new Long[] {docs.getId(), h2.getId(), work.getId(), travel.getId()};
    });
    docsId = ids[0];
    h2Id = ids[1];
    workId = ids[2];
    travelId = ids[3];
    sql.clear();
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  private List<String> rows(EntityManagerFactory factory, String query) {
    return Database.nativeRows(factory, query).stream()
        .map(r -> r instanceof Object[] a ? Arrays.toString(a) : String.valueOf(r))
        .toList();
  }

  private List<String> titles() {
    return emf.callInTransaction(em -> em
        .createQuery("select b.title from Bookmark b order by b.title", String.class)
        .getResultList());
  }

  private boolean ran(String fragment) {
    return sql.stream().anyMatch(s -> s.contains(fragment));
  }

  // ---- defaults: column, insert, remove ----

  @Test
  void insertWritesFalseIntoTheDeletedColumn() {
    emf.runInTransaction(em -> em.persist(new Bookmark("News", "https://news.example.com")));
    assertTrue(ran("insert into Bookmark (folder_id,title,url,deleted,id) values (?,?,?,false,?)"));
  }

  @Test
  void insertWritesNullIntoTheTimestampColumn() {
    emf.runInTransaction(em -> em.persist(new BookmarkFolder("News")));
    assertTrue(ran("insert into BookmarkFolder (name,deleted,id) values (?,null,?)"));
  }

  @Test
  void removeRunsAnUpdateInsteadOfADelete() {
    emf.runInTransaction(em -> em.remove(em.find(Bookmark.class, h2Id)));
    assertTrue(ran("update Bookmark set deleted=true where id=? and deleted=false"));
    assertTrue(ran("update bookmark_tag set deleted=true where bookmark_id=? and deleted=false"));
    assertFalse(sql.stream().anyMatch(s -> s.startsWith("delete")));
    assertEquals(List.of("[H2 Database, true]", "[Hibernate Docs, false]", "[Hotels, false]", "[Train Times, false]"),
        rows(emf, "select title, deleted from Bookmark order by title"));
  }

  @Test
  void findAndQueriesSkipDeletedRows() {
    emf.runInTransaction(em -> em.remove(em.find(Bookmark.class, h2Id)));
    sql.clear();
    assertNull(emf.callInTransaction(em -> em.find(Bookmark.class, h2Id)));
    assertTrue(ran("from Bookmark b1_0 where b1_0.deleted=false and b1_0.id=?"));
    assertEquals(List.of("Hibernate Docs", "Hotels", "Train Times"), titles());
    Long count = emf.callInTransaction(em ->
        em.createQuery("select count(*) from Bookmark", Long.class).getSingleResult());
    assertEquals(3L, count);
    assertEquals(List.of("4"), rows(emf, "select count(*) from Bookmark"));
  }

  @Test
  void joinsSkipDeletedTargets() {
    emf.runInTransaction(em -> em.remove(em.find(BookmarkFolder.class, travelId)));
    sql.clear();
    List<Object[]> joined = emf.callInTransaction(em -> em
        .createQuery("select b.title, f.name from Bookmark b join b.folder f order by b.title", Object[].class)
        .getResultList());
    assertEquals(List.of("[H2 Database, Work]", "[Hibernate Docs, Work]"),
        joined.stream().map(Arrays::toString).toList());
    assertTrue(ran("join BookmarkFolder f1_0 on f1_0.id=b1_0.folder_id and f1_0.deleted is null where b1_0.deleted=false"));
  }

  @Test
  void nativeUpdateRestoresARow() {
    emf.runInTransaction(em -> em.remove(em.find(Bookmark.class, h2Id)));
    emf.runInTransaction(em -> em
        .createNativeQuery("update Bookmark set deleted = false where id = ?")
        .setParameter(1, h2Id)
        .executeUpdate());
    Bookmark h2 = emf.callInTransaction(em -> em.find(Bookmark.class, h2Id));
    assertNotNull(h2);
    assertEquals("H2 Database", h2.getTitle());
  }

  @Test
  void nativeQueryListsDeletedRows() {
    emf.runInTransaction(em -> em.remove(em.find(Bookmark.class, h2Id)));
    List<Bookmark> trash = emf.callInTransaction(em -> em
        .createNativeQuery("select * from Bookmark where deleted = true", Bookmark.class)
        .getResultList());
    assertEquals(1, trash.size());
    assertEquals("H2 Database", trash.get(0).getTitle());
  }

  // ---- collections ----

  @Test
  void removingATagSoftDeletesOnlyThatRow() {
    emf.runInTransaction(em -> em.find(Bookmark.class, docsId).getTags().remove("docs"));
    assertTrue(ran("update bookmark_tag set deleted=true where bookmark_id=? and tag=? and deleted=false"));
    assertEquals(List.of("[docs, true]", "[java, false]"), rows(emf, "select tag, deleted from bookmark_tag order by tag"));
    assertEquals("[java]", emf.callInTransaction(em -> em.find(Bookmark.class, docsId).getTags().toString()));
  }

  @Test
  void reAddingARemovedTagBreaksTheSetKey() {
    emf.runInTransaction(em -> em.find(Bookmark.class, docsId).getTags().remove("docs"));
    RollbackException e = assertThrows(RollbackException.class,
        () -> emf.runInTransaction(em -> em.find(Bookmark.class, docsId).getTags().add("docs")));
    assertTrue(e.getMessage().contains("Unique index or primary key violation"));
    assertTrue(e.getMessage().contains("BOOKMARK_TAG(BOOKMARK_ID NULLS FIRST, TAG NULLS FIRST)"));
  }

  @Test
  void listCollectionRewritesAllRows() {
    try (EntityManagerFactory lists = Database.create("lists", false, inspector, TagListBookmark.class)) {
      Long id = lists.callInTransaction(em -> {
        TagListBookmark docs = new TagListBookmark("Hibernate Docs");
        docs.tags.add("java");
        docs.tags.add("docs");
        em.persist(docs);
        return docs.id;
      });
      lists.runInTransaction(em -> em.find(TagListBookmark.class, id).tags.remove("docs"));
      lists.runInTransaction(em -> em.find(TagListBookmark.class, id).tags.add("docs"));
      assertEquals(List.of("[docs, false]", "[docs, true]", "[java, false]", "[java, true]", "[java, true]"),
          rows(lists, "select tag, deleted from bookmark_tag order by tag, deleted"));
    }
  }

  @Test
  void manyToManySoftDeletesTheJoinRowOnly() {
    try (EntityManagerFactory m2m = Database.create("m2m", false, inspector,
        Profile.class, Bookmark.class, BookmarkFolder.class)) {
      Long[] ids = m2m.callInTransaction(em -> {
        Bookmark hotels = new Bookmark("Hotels", "https://hotels.example.com");
        em.persist(hotels);
        Profile lokesh = new Profile("Lokesh");
        lokesh.bookmarks.add(hotels);
        em.persist(lokesh);
        return new Long[] {lokesh.id, hotels.getId()};
      });
      sql.clear();
      m2m.runInTransaction(em -> em.find(Profile.class, ids[0]).bookmarks.clear());
      assertTrue(ran("update profile_bookmark set deleted=true where profile_id=? and deleted=false"));
      assertEquals(List.of("true"), rows(m2m, "select deleted from profile_bookmark"));
      assertNotNull(m2m.callInTransaction(em -> em.find(Bookmark.class, ids[1])));
    }
  }

  @Test
  void softDeleteOnOneToManyFailsAtStartup() {
    UnsupportedMappingException e = assertThrows(UnsupportedMappingException.class,
        () -> Database.create("bad1", false, OneToManySoftDeleteFolder.class, Bookmark.class, BookmarkFolder.class));
    assertEquals("@SoftDelete cannot be applied to @OneToMany - "
        + "com.howtodoinjava.hibernate.softdelete.OneToManySoftDeleteFolder.bookmarks", e.getMessage());
  }

  // ---- TIMESTAMP strategy on the folder, cascade ----

  @Test
  void removingAFolderSetsTheTimestampAndCascades() {
    emf.runInTransaction(em -> em.remove(em.find(BookmarkFolder.class, travelId)));
    assertTrue(ran("update BookmarkFolder set deleted=localtimestamp where id=? and deleted is null"));
    assertEquals(2, sql.stream().filter(s -> s.equals("update Bookmark set deleted=true where id=? and deleted=false")).count());
    assertEquals(List.of("[Travel, true]", "[Work, false]"),
        rows(emf, "select name, deleted is not null from BookmarkFolder order by name"));
    assertEquals(List.of("[H2 Database, false]", "[Hibernate Docs, false]", "[Hotels, true]", "[Train Times, true]"),
        rows(emf, "select title, deleted from Bookmark order by title"));
  }

  @Test
  void restoringAFolderDoesNotRestoreItsBookmarks() {
    emf.runInTransaction(em -> em.remove(em.find(BookmarkFolder.class, travelId)));
    emf.runInTransaction(em -> em
        .createNativeQuery("update BookmarkFolder set deleted = null where id = ?")
        .setParameter(1, travelId)
        .executeUpdate());
    assertEquals("Travel", emf.callInTransaction(em -> em.find(BookmarkFolder.class, travelId).getName()));
    Integer before = emf.callInTransaction(em -> em.find(BookmarkFolder.class, travelId).getBookmarks().size());
    assertEquals(0, before);

    emf.runInTransaction(em -> em
        .createNativeQuery("update Bookmark set deleted = false where folder_id = ?")
        .setParameter(1, travelId)
        .executeUpdate());
    String after = emf.callInTransaction(em -> em.find(BookmarkFolder.class, travelId).getBookmarks().toString());
    assertEquals("[Train Times, Hotels]", after);
  }

  @Test
  void timestampColumnHoldsWhenTheRowWasDeleted() {
    emf.runInTransaction(em -> em.remove(em.find(BookmarkFolder.class, travelId)));
    assertEquals(List.of("[Travel, TIMESTAMP WITH TIME ZONE]"), rows(emf,
        "select name, data_type from BookmarkFolder, information_schema.columns "
            + "where table_name = 'BOOKMARKFOLDER' and column_name = 'DELETED' and deleted is not null"));
  }

  @Test
  void lazyManyToOneToASoftDeletedEntityIsLoadedRightAway() {
    sql.clear();
    emf.runInTransaction(em -> em.find(Bookmark.class, h2Id));
    assertEquals(2, sql.size());
    assertTrue(sql.get(1).contains("from BookmarkFolder bf1_0 where bf1_0.deleted is null and bf1_0.id=?"));
  }

  @Test
  void lazyManyToOneToAPlainEntityStaysLazy() {
    try (EntityManagerFactory plain = Database.create("plain", false, inspector, PlainFolder.class, PlainFolderBookmark.class)) {
      Long id = plain.callInTransaction(em -> {
        PlainFolder work = new PlainFolder("Work");
        em.persist(work);
        PlainFolderBookmark docs = new PlainFolderBookmark("Hibernate Docs", work);
        em.persist(docs);
        return docs.id;
      });
      sql.clear();
      plain.runInTransaction(em -> em.find(PlainFolderBookmark.class, id));
      assertEquals(1, sql.size());
    }
  }

  @Test
  void manyToOneToADeletedFolderIsNull() {
    emf.runInTransaction(em -> em.createNativeQuery("update BookmarkFolder set deleted = current_timestamp where id = ?")
        .setParameter(1, workId).executeUpdate());
    assertNull(emf.callInTransaction(em -> em.find(Bookmark.class, h2Id).getFolder()));
  }

  // ---- bulk delete ----

  @Test
  void jpqlBulkDeleteBecomesAnUpdate() {
    Integer rows = emf.callInTransaction(em -> em
        .createQuery("delete from Bookmark where url like '%hibernate%'")
        .executeUpdate());
    assertEquals(1, rows);
    assertTrue(ran("update Bookmark b1_0 set deleted=true where b1_0.url like '%hibernate%' escape '' and b1_0.deleted=false"));
    assertTrue(ran("update bookmark_tag to_delete_ set deleted=true where to_delete_.bookmark_id in"));
    assertEquals(List.of("4"), rows(emf, "select count(*) from Bookmark"));
  }

  // ---- strategies and converters ----

  @Test
  void activeStrategyStoresTrueForLiveRows() {
    try (EntityManagerFactory active = Database.create("active", false, inspector, ActiveBookmark.class)) {
      Long id = active.callInTransaction(em -> {
        ActiveBookmark hotels = new ActiveBookmark("Hotels", "https://hotels.example.com");
        em.persist(hotels);
        return hotels.getId();
      });
      active.runInTransaction(em -> em.remove(em.find(ActiveBookmark.class, id)));
      assertTrue(ran("insert into Bookmark (title,url,active,id) values (?,?,true,?)"));
      assertTrue(ran("update Bookmark set active=false where id=? and active=true"));
      assertEquals(List.of("[Hotels, false]"), rows(active, "select title, active from Bookmark"));
    }
  }

  @Test
  void columnNameAndYesNoConverter() {
    try (EntityManagerFactory yesNo = Database.create("yesno", false, inspector, YesNoBookmark.class)) {
      Long id = yesNo.callInTransaction(em -> {
        YesNoBookmark hotels = new YesNoBookmark("Hotels", "https://hotels.example.com");
        em.persist(hotels);
        return hotels.getId();
      });
      yesNo.runInTransaction(em -> em.remove(em.find(YesNoBookmark.class, id)));
      assertTrue(ran("insert into Bookmark (title,url,removed,id) values (?,?,'N',?)"));
      assertTrue(ran("update Bookmark set removed='Y' where id=? and removed='N'"));
      assertEquals(List.of("[Hotels, Y]"), rows(yesNo, "select title, removed from Bookmark"));
    }
  }

  @Test
  void builtInConverterValues() {
    assertEquals(1, new NumericBooleanConverter().toRelationalValue(true));
    assertEquals(0, new NumericBooleanConverter().toRelationalValue(false));
    assertEquals('T', new TrueFalseConverter().toRelationalValue(true));
    assertEquals('F', new TrueFalseConverter().toRelationalValue(false));
    assertEquals('Y', new YesNoConverter().toRelationalValue(true));
    assertEquals('N', new YesNoConverter().toRelationalValue(false));
  }

  @Test
  void converterWithTimestampFailsAtStartup() {
    UnsupportedMappingException e = assertThrows(UnsupportedMappingException.class,
        () -> Database.create("bad2", false, TimestampConverterBookmark.class));
    assertEquals("Specifying SoftDelete#converter in conjunction with SoftDeleteType.TIMESTAMP is not supported",
        e.getMessage());
  }

  @Test
  void defaultColumnIsANotNullBoolean() {
    assertEquals(List.of("[BOOLEAN, NO]"), rows(emf,
        "select data_type, is_nullable from information_schema.columns "
            + "where table_name = 'BOOKMARK' and column_name = 'DELETED'"));
  }

  @Test
  void readOnlyFlagFieldIsAlwaysFalse() {
    try (EntityManagerFactory flag = Database.create("flag", false, FlagFieldBookmark.class)) {
      Long id = flag.callInTransaction(em -> {
        FlagFieldBookmark docs = new FlagFieldBookmark("Hibernate Docs");
        em.persist(docs);
        return docs.id;
      });
      Boolean flagValue = flag.callInTransaction(em -> em.find(FlagFieldBookmark.class, id).deleted);
      assertFalse(flagValue);
      flag.runInTransaction(em -> em.remove(em.find(FlagFieldBookmark.class, id)));
      assertNull(flag.callInTransaction(em -> em.find(FlagFieldBookmark.class, id)));
    }
  }

  // ---- the older approach ----

  @Test
  void sqlDeleteAndSqlRestriction() {
    try (EntityManagerFactory legacy = Database.create("legacy", false, inspector, LegacyBookmark.class)) {
      Long[] ids = legacy.callInTransaction(em -> {
        LegacyBookmark hotels = new LegacyBookmark("Hotels", "https://hotels.example.com");
        LegacyBookmark trains = new LegacyBookmark("Train Times", "https://trains.example.com");
        em.persist(hotels);
        em.persist(trains);
        return new Long[] {hotels.getId(), trains.getId()};
      });
      legacy.runInTransaction(em -> em.remove(em.find(LegacyBookmark.class, ids[0])));
      assertTrue(ran("update Bookmark set deleted = true where id = ?"));
      assertNull(legacy.callInTransaction(em -> em.find(LegacyBookmark.class, ids[0])));
      assertTrue(ran("where lb1_0.id=? and (lb1_0.deleted = false)"));

      // a JPQL bulk delete ignores @SQLDelete and removes the row for real
      Integer rows = legacy.callInTransaction(em -> em
          .createQuery("delete from Bookmark where title = 'Train Times'")
          .executeUpdate());
      assertEquals(1, rows);
      assertTrue(ran("delete from Bookmark lb1_0 where lb1_0.title='Train Times' and (lb1_0.deleted = false)"));
      assertEquals(List.of("[Hotels, true]"), rows(legacy, "select title, deleted from Bookmark"));
    }
  }

  @Test
  void legacyEntityKeepsTheOldFlagInMemory() {
    try (EntityManagerFactory legacy = Database.create("legacy2", false, LegacyBookmark.class)) {
      LegacyBookmark removed = legacy.callInTransaction(em -> {
        LegacyBookmark hotels = new LegacyBookmark("Hotels", "https://hotels.example.com");
        em.persist(hotels);
        em.flush();
        em.remove(hotels);
        em.flush();
        return hotels;
      });
      assertFalse(removed.isDeleted());
    }
  }

  // ---- unique constraints ----

  @Test
  void plainUniqueColumnBlocksReusingADeletedName() {
    try (EntityManagerFactory unique = Database.create("unique", false, UniqueNameFolder.class)) {
      Long id = unique.callInTransaction(em -> {
        UniqueNameFolder travel = new UniqueNameFolder("Travel");
        em.persist(travel);
        return travel.getId();
      });
      unique.runInTransaction(em -> em.remove(em.find(UniqueNameFolder.class, id)));
      RollbackException e = assertThrows(RollbackException.class,
          () -> unique.runInTransaction(em -> em.persist(new UniqueNameFolder("Travel"))));
      assertTrue(e.getMessage().contains("Unique index or primary key violation"));
      assertTrue(e.getMessage().contains("BOOKMARKFOLDER(NAME NULLS FIRST) VALUES ( /* 1 */ 'Travel' )"));
    }
  }

  @Test
  void uniqueAmongActiveRowsAllowsReuse() {
    emf.runInTransaction(em -> em.createNativeQuery(SoftDeleteDemo.UNIQUE_ACTIVE_NAME).executeUpdate());
    emf.runInTransaction(em -> em.remove(em.find(BookmarkFolder.class, travelId)));
    emf.runInTransaction(em -> em.persist(new BookmarkFolder("Travel")));
    // a second deletion of the new folder has a different timestamp, so it is allowed too
    Long second = emf.callInTransaction(em -> em
        .createQuery("select f.id from BookmarkFolder f where f.name = 'Travel'", Long.class)
        .getSingleResult());
    emf.runInTransaction(em -> em.remove(em.find(BookmarkFolder.class, second)));
    emf.runInTransaction(em -> em.persist(new BookmarkFolder("Travel")));
    assertEquals(List.of("3"), rows(emf, "select count(*) from BookmarkFolder where name = 'Travel'"));

    // two active folders with the same name are still rejected
    RollbackException e = assertThrows(RollbackException.class,
        () -> emf.runInTransaction(em -> em.persist(new BookmarkFolder("Travel"))));
    assertTrue(e.getMessage().contains("UK_FOLDER_NAME"));
    assertTrue(e.getMessage().contains("VALUES ( /* key:"));
  }

  @Test
  void uniqueOnNameAndBooleanFlagFailsOnTheSecondDelete() {
    emf.runInTransaction(em -> em.createNativeQuery(
        "alter table Bookmark add constraint uk_bookmark_url unique (url, deleted)").executeUpdate());
    emf.runInTransaction(em -> em.remove(em.find(Bookmark.class, h2Id)));
    emf.runInTransaction(em -> {
      Bookmark again = new Bookmark("H2 Database", "https://h2database.com");
      em.persist(again);
    });
    Long again = emf.callInTransaction(em -> em
        .createQuery("select b.id from Bookmark b where b.url = 'https://h2database.com'", Long.class)
        .getSingleResult());
    RollbackException e = assertThrows(RollbackException.class,
        () -> emf.runInTransaction(em -> em.remove(em.find(Bookmark.class, again))));
    assertTrue(e.getMessage().contains("UK_BOOKMARK_URL"));
  }

  @Test
  void withoutSoftDeleteRemoveRunsADelete() {
    try (EntityManagerFactory hard = Database.create("hard", false, inspector, HardDeleteBookmark.class)) {
      Long id = hard.callInTransaction(em -> {
        HardDeleteBookmark hotels = new HardDeleteBookmark("Hotels");
        em.persist(hotels);
        return hotels.id;
      });
      hard.runInTransaction(em -> em.remove(em.find(HardDeleteBookmark.class, id)));
      assertTrue(ran("delete from Bookmark where id=?"));
      assertEquals(List.of("0"), rows(hard, "select count(*) from Bookmark"));
    }
  }

  @Test
  void demoRunsWithoutErrors() {
    assertDoesNotThrow(() -> SoftDeleteDemo.main(new String[0]));
  }
}
