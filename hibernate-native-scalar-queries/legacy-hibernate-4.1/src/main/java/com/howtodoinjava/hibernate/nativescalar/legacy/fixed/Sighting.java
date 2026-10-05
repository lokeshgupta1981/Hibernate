package com.howtodoinjava.hibernate.nativescalar.legacy.fixed;

import java.util.Date;
import javax.persistence.ColumnResult;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

// The workaround for Hibernate 3.6 to 4.1: every scalar named native query
// references a @SqlResultSetMapping with @ColumnResult.
@Entity
@SqlResultSetMappings({
    @SqlResultSetMapping(name = "totalMapping", columns = @ColumnResult(name = "total")),
    @SqlResultSetMapping(name = "speciesTotalMapping",
        columns = {@ColumnResult(name = "species"), @ColumnResult(name = "total")})
})
@NamedNativeQueries({
    @NamedNativeQuery(name = "Sighting.countBySpecies",
        query = "select count(*) as total from Sighting where species = ?",
        resultSetMapping = "totalMapping"),
    @NamedNativeQuery(name = "Sighting.totalsBySpecies",
        query = "select species, sum(count) as total from Sighting group by species order by species",
        resultSetMapping = "speciesTotalMapping"),
    @NamedNativeQuery(name = "Sighting.renameLocation",
        query = "update Sighting set location = ? where location = ?",
        resultSetMapping = "totalMapping")
})
public class Sighting {

  @Id
  @GeneratedValue
  private Long id;

  private String species;
  private String location;
  private int count;

  @Temporal(TemporalType.DATE)
  private Date seenOn;

  protected Sighting() {
  }

  public Sighting(String species, String location, int count, Date seenOn) {
    this.species = species;
    this.location = location;
    this.count = count;
    this.seenOn = seenOn;
  }
}
