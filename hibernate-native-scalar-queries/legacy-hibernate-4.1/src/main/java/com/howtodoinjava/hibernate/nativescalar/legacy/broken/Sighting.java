package com.howtodoinjava.hibernate.nativescalar.legacy.broken;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

// Hibernate 3.6 to 4.1: a named native query without resultClass or resultSetMapping
// stops the EntityManagerFactory with NotYetImplementedException.
@Entity
@NamedNativeQuery(name = "Sighting.countBySpecies",
    query = "select count(*) from Sighting where species = ?")
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
