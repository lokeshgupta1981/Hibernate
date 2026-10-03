# Hibernate Many to Many Mapping

Source code for the article [Hibernate Many to Many Mapping with JPA @ManyToMany Examples](https://howtodoinjava.com/hibernate/hibernate-many-to-many-mapping/).

## Versions

- Java 25
- Hibernate ORM 7.4.11.Final (Jakarta Persistence 3.2)
- H2 2.5.252 (in-memory database)
- JUnit 6.1.3
- Maven 3.9 or newer

## Run

```bash
mvn -q compile exec:java   # prints the SQL for each step
mvn test                   # asserts every result shown in the article
```

## Files

| File | What it shows |
|---|---|
| Playlist.java | Owning side: `@ManyToMany` with `@JoinTable(name = "playlist_song")`, a `Set<Song>` and helper methods that sync both sides |
| Song.java | Inverse side: `@ManyToMany(mappedBy = "songs")` |
| bag/Playlist.java, bag/Song.java | The mapping to avoid: `List<Song>` (a bag), `CascadeType.ALL`, default join table names |
| entry/PlaylistSong.java | Join entity with `@EmbeddedId`, two `@ManyToOne` with `@MapsId`, and the extra columns `added_on` and `position` |
| entry/PlaylistSongId.java | `@Embeddable` composite key with `equals()` and `hashCode()` |
| entry/Playlist.java, entry/Song.java | `@OneToMany` to the join entity, `@OrderBy("position")` |
| entry/PlaylistItem.java | Join entity with its own generated id, so the same song can appear twice |
| Database.java | Bootstraps Hibernate with `HibernatePersistenceConfiguration` (no persistence.xml) |
| SqlLog.java | `StatementInspector` that records the SQL so the tests can assert it |
| ManyToManyDemo.java | Runs each step and prints the SQL |
| ManyToManyTest.java | JUnit tests for every behavior described in the article |
