package com.howtodoinjava.hibernate.mergerefresh;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.h2.api.Trigger;

/**
 * H2 trigger: a new shipment without a status gets the status "received".
 * It stands for any value the database sets on its own (triggers, defaults, procedures).
 */
public class ReceivedTrigger implements Trigger {

  private int statusIndex;

  @Override
  public void init(Connection conn, String schema, String trigger, String table, boolean before, int type)
      throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement(
        "select ordinal_position from information_schema.columns where table_name = ? and column_name = 'STATUS'")) {
      ps.setString(1, table);
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        statusIndex = rs.getInt(1) - 1;
      }
    }
  }

  @Override
  public void fire(Connection conn, Object[] oldRow, Object[] newRow) {
    if (newRow[statusIndex] == null) {
      newRow[statusIndex] = "received";
    }
  }
}
