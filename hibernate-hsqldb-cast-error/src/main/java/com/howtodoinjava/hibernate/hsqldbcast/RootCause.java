package com.howtodoinjava.hibernate.hsqldbcast;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class RootCause {

  private RootCause() {
  }

  /** Returns the first SQLException in the cause chain, or null. */
  public static SQLException sqlException(Throwable error) {
    for (Throwable cause = error; cause != null; cause = cause.getCause()) {
      if (cause instanceof SQLException sql) {
        return sql;
      }
    }
    return null;
  }

  /** One line per exception in the chain: class name and message. */
  public static List<String> chain(Throwable error) {
    List<String> lines = new ArrayList<>();
    for (Throwable cause = error; cause != null; cause = cause.getCause()) {
      lines.add(cause.getClass().getName() + ": " + cause.getMessage());
    }
    return lines;
  }
}
