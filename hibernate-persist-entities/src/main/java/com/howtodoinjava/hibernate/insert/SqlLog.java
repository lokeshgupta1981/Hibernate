package com.howtodoinjava.hibernate.insert;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/** Records every SQL statement so the tests can check when Hibernate sends it. */
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

  public static List<String> statements() {
    synchronized (STATEMENTS) {
      return List.copyOf(STATEMENTS);
    }
  }
}
