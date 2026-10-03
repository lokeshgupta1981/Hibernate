package com.howtodoinjava.hibernate.genericjdbc;

import java.sql.SQLException;
import org.hibernate.JDBCException;

/**
 * Finds the JDBCException in a cause chain and prints the SQLException behind it.
 */
public final class RootCause {

  private RootCause() {
  }

  public static JDBCException jdbcException(Throwable error) {
    for (Throwable cause = error; cause != null; cause = cause.getCause()) {
      if (cause instanceof JDBCException jdbc) {
        return jdbc;
      }
    }
    return null;
  }

  public static String describe(Throwable error) {
    JDBCException jdbc = jdbcException(error);
    if (jdbc == null) {
      return "no JDBCException in " + error;
    }
    SQLException sqlException = jdbc.getSQLException();
    return "Hibernate exception: " + jdbc.getClass().getSimpleName() + "\n"
        + "SQL:                 " + jdbc.getSQL() + "\n"
        + "SQLException:        " + sqlException.getClass().getName() + "\n"
        + "SQLState:            " + sqlException.getSQLState() + "\n"
        + "Error code:          " + sqlException.getErrorCode() + "\n"
        + "Message:             " + sqlException.getMessage().lines().findFirst().orElse("");
  }
}
