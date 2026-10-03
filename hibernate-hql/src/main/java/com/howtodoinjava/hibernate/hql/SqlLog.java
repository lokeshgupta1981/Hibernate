package com.howtodoinjava.hibernate.hql;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/** Records every SQL statement Hibernate sends, so the tests can check it. */
public class SqlLog implements StatementInspector {

  private static final List<String> STATEMENTS = new ArrayList<>();

  @Override
  public String inspect(String sql) {
    STATEMENTS.add(sql);
    return sql;
  }

  public static void clear() {
    STATEMENTS.clear();
  }

  public static List<String> statements() {
    return List.copyOf(STATEMENTS);
  }

  public static String last() {
    return STATEMENTS.getLast();
  }
}
