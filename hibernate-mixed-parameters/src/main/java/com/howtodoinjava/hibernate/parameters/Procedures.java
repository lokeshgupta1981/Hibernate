package com.howtodoinjava.hibernate.parameters;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Java methods that H2 exposes as stored procedures with CREATE ALIAS.
 * On MySQL or PostgreSQL the same procedure would be written with CREATE PROCEDURE.
 */
public final class Procedures {

  private Procedures() {
  }

  /** Counts the trains that leave a station at or after the given hour. */
  public static int countDepartures(Connection con, String fromStation, int hour) throws SQLException {
    String sql = "select count(*) from TrainService where fromStation = ? and hour(departsAt) >= ?";
    try (PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, fromStation);
      ps.setInt(2, hour);
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getInt(1);
      }
    }
  }
}
