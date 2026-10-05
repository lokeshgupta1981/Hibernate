package com.howtodoinjava.hibernate.namedqueryerrors;

import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQuery;

/**
 * Each nested class carries one broken named query. A class is added to the persistence unit
 * next to Volunteer and Shift to reproduce one startup error at a time.
 */
public final class BrokenQueries {

  private BrokenQueries() {
  }

  /** Table name instead of the entity name Shift. */
  @NamedQuery(name = "Shift.findAll", query = "select s from volunteer_shift s")
  public static class TableName {
  }

  /** Column name instead of the Java field name startsAt. */
  @NamedQuery(name = "Shift.findStartingAfter",
      query = "select s from Shift s where s.starts_at > :from")
  public static class ColumnName {
  }

  /** Misspelled attribute of the joined entity. */
  @NamedQuery(name = "Shift.findByVolunteerName",
      query = "select s from Shift s where s.volunteer.nmae = :name")
  public static class AttributeTypo {
  }

  /** A JPQL query for an entity that is not part of the persistence unit. */
  @NamedQuery(name = "Shift.findAll", query = "select s from Shift s")
  public static class UnregisteredEntity {
  }

  /** Plain SQL in a JPQL named query. */
  @NamedQuery(name = "Shift.findLongShifts", query = "select * from volunteer_shift where hours >= 4")
  public static class SqlInJpql {
  }

  /** Misspelled keyword. */
  @NamedQuery(name = "Shift.findAll", query = "select s form Shift s")
  public static class KeywordTypo {
  }

  /** A semicolon at the end, copied from an SQL console. */
  @NamedQuery(name = "Shift.findMinHours",
      query = "select s from Shift s where s.hours >= :hours;")
  public static class TrailingSemicolon {
  }

  /** JDBC-style parameter without a number. */
  @NamedQuery(name = "Shift.findMinHours", query = "select s from Shift s where s.hours >= ?")
  public static class UnlabeledParameter {
  }

  /** Ordinal parameters start at ?1, not ?0. */
  @NamedQuery(name = "Shift.findMinHours", query = "select s from Shift s where s.hours >= ?0")
  public static class ZeroParameter {
  }

  /** A string literal compared with an int attribute. */
  @NamedQuery(name = "Shift.findFourHours", query = "select s from Shift s where s.hours = 'four'")
  public static class TypeMismatch {
  }

  /** Two broken queries: Hibernate reports both. */
  @NamedQuery(name = "Shift.findAll", query = "select s from volunteer_shift s")
  @NamedQuery(name = "Shift.findStartingAfter",
      query = "select s from Shift s where s.starts_at > :from")
  public static class TwoErrors {
  }

  /** Native SQL with a wrong table name: not checked at startup. */
  @NamedNativeQuery(name = "Shift.findAllSql", query = "select * from shifts",
      resultClass = Shift.class)
  public static class NativeWrongTable {
  }
}
