package com.howtodoinjava.hibernate.firstlevelcache;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Records every SQL statement Hibernate prepares, so the demo and the tests
 * can count how many statements reached the database.
 */
public class SqlCounter implements StatementInspector {

  private static final List<String> STATEMENTS = new CopyOnWriteArrayList<>();

  @Override
  public String inspect(String sql) {
    STATEMENTS.add(sql);
    return sql;
  }

  public static void reset() {
    STATEMENTS.clear();
  }

  public static int count() {
    return STATEMENTS.size();
  }

  public static long selects() {
    return STATEMENTS.stream().filter(s -> s.startsWith("select")).count();
  }

  public static List<String> statements() {
    return List.copyOf(STATEMENTS);
  }
}
