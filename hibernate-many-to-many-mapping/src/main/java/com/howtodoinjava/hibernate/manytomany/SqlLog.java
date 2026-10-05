package com.howtodoinjava.hibernate.manytomany;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/** Records every SQL statement so the tests can assert the exact statements. */
public class SqlLog implements StatementInspector {

  private static final List<String> STATEMENTS = new ArrayList<>();

  @Override
  public String inspect(String sql) {
    synchronized (STATEMENTS) {
      STATEMENTS.add(sql);
    }
    return sql;
  }

  public static void clear() {
    synchronized (STATEMENTS) {
      STATEMENTS.clear();
    }
  }

  public static int allCount() {
    synchronized (STATEMENTS) {
      return STATEMENTS.size();
    }
  }

  /** Statements that change data (insert, update, delete), in the order they ran. */
  public static List<String> writes() {
    synchronized (STATEMENTS) {
      return STATEMENTS.stream()
          .filter(s -> !s.startsWith("select"))
          .toList();
    }
  }
}
