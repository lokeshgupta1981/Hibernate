package com.howtodoinjava.hibernate.nativescalar.legacy.brokenupdate;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

// Hibernate 3.6 to 4.1: the check does not look at the SQL, so an UPDATE
// without resultClass or resultSetMapping fails the same way.
@Entity
@NamedNativeQuery(name = "Sighting.renameLocation",
    query = "update Sighting set location = ? where location = ?")
public class Sighting {

  @Id
  @GeneratedValue
  private Long id;

  private String species;
  private String location;
  private int count;

  @Temporal(TemporalType.DATE)
  private Date seenOn;
}
