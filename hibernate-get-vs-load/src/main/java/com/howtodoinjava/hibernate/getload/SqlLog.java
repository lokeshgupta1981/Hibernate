package com.howtodoinjava.hibernate.getload;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/** Records every SQL statement Hibernate prepares, so the tests can assert the exact SQL. */
public class SqlLog implements StatementInspector {

  private static final List<String> STATEMENTS = new CopyOnWriteArrayList<>();

  @Override
  public String inspect(String sql) {
    STATEMENTS.add(sql);
    return sql;
  }

  public static List<String> statements() {
    return List.copyOf(STATEMENTS);
  }

  public static void clear() {
    STATEMENTS.clear();
  }
}
