package com.howtodoinjava.hibernate.nativescalar;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SqlResultSetMapping;
import java.time.LocalDate;

@Entity
// 1. No resultClass, no resultSetMapping: Hibernate 4.1 and older failed at startup
@NamedNativeQuery(name = "Sighting.countBySpecies",
    query = "select count(*) from Sighting where species = :species")
@NamedNativeQuery(name = "Sighting.totalsBySpecies",
    query = "select species, sum(count) as total from Sighting group by species order by species")
@NamedNativeQuery(name = "Sighting.renameLocation",
    query = "update Sighting set location = :newName where location = :oldName")
// 2. A basic type as resultClass
@NamedNativeQuery(name = "Sighting.lastSeen",
    query = "select max(seenOn) from Sighting where species = :species",
    resultClass = LocalDate.class)
// 3. The old workaround, still valid: a mapping with @ColumnResult
@NamedNativeQuery(name = "Sighting.totalsByLocation",
    query = "select location, sum(count) as total from Sighting group by location order by location",
    resultSetMapping = "LocationTotalMapping")
@SqlResultSetMapping(name = "LocationTotalMapping",
    columns = {
        @ColumnResult(name = "location"),
        @ColumnResult(name = "total", type = Integer.class)})
// 4. The Hibernate 4.1 workaround after the upgrade: still accepted, no longer needed
@SqlResultSetMapping(name = "totalMapping", columns = @ColumnResult(name = "total"))
@NamedNativeQuery(name = "Sighting.countBySpeciesMapped",
    query = "select count(*) as total from Sighting where species = :species",
    resultSetMapping = "totalMapping")
@NamedNativeQuery(name = "Sighting.renameLocationMapped",
    query = "update Sighting set location = :newName where location = :oldName",
    resultSetMapping = "totalMapping")
// 5. JPQL alternative
@NamedQuery(name = "Sighting.countJpql",
    query = "select count(s) from Sighting s where s.species = :species")
public class Sighting {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String species;
  private String location;
  private int count;
  private LocalDate seenOn;

  protected Sighting() {
  }

  public Sighting(String species, String location, int count, LocalDate seenOn) {
    this.species = species;
    this.location = location;
    this.count = count;
    this.seenOn = seenOn;
  }

  public Long getId() {
    return id;
  }

  public String getSpecies() {
    return species;
  }

  public String getLocation() {
    return location;
  }

  public int getCount() {
    return count;
  }

  public LocalDate getSeenOn() {
    return seenOn;
  }
}
