package com.howtodoinjava.hibernate.namedquery;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SqlResultSetMapping;
import java.math.BigDecimal;

@Entity
@NamedQuery(name = "Room.findAvailableByType",
    query = "select r from Room r where r.type = :type and r.available = true order by r.number",
    resultClass = Room.class)
@NamedQuery(name = "Room.countAvailable",
    query = "select count(r) from Room r where r.available = true")
@NamedQuery(name = "Room.findByRateBetween",
    query = "select r from Room r where r.nightlyRate between ?1 and ?2 order by r.number")
@NamedQuery(name = "Room.findByTypes",
    query = "select r from Room r where r.type in :types order by r.number")
@NamedQuery(name = "Room.findRates",
    query = "select r.number, r.nightlyRate from Room r where r.type = :type order by r.number")
@NamedQuery(name = "Room.updateRate",
    query = "update Room r set r.nightlyRate = :rate where r.type = :type")
@NamedNativeQuery(name = "Room.findFreeUnderRate",
    query = "select * from Room where available = true and nightlyRate <= ? order by number",
    resultClass = Room.class)
@NamedNativeQuery(name = "Room.countByType",
    query = "select type, count(*) as rooms from Room group by type order by type",
    resultSetMapping = "RoomCountMapping")
@SqlResultSetMapping(name = "RoomCountMapping",
    classes = @ConstructorResult(targetClass = RoomCount.class,
        columns = {@ColumnResult(name = "type"), @ColumnResult(name = "rooms", type = Long.class)}))
public class Room {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Integer number;
  private String type;
  private BigDecimal nightlyRate;
  private boolean available;

  protected Room() {
  }

  public Room(Integer number, String type, String nightlyRate, boolean available) {
    this.number = number;
    this.type = type;
    this.nightlyRate = new BigDecimal(nightlyRate);
    this.available = available;
  }

  public Long getId() {
    return id;
  }

  public Integer getNumber() {
    return number;
  }

  public String getType() {
    return type;
  }

  public BigDecimal getNightlyRate() {
    return nightlyRate;
  }

  public boolean isAvailable() {
    return available;
  }

  public void setAvailable(boolean available) {
    this.available = available;
  }

  @Override
  public String toString() {
    return number + " " + type + " " + nightlyRate;
  }
}
