package com.howtodoinjava.hibernate.onetomany;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/** Records every SQL statement Hibernate sends, so tests can count them. */
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

  /** Number of statements that start with the given word, for example "insert". */
  public static long count(String verb) {
    return statements().stream().filter(s -> s.toLowerCase().startsWith(verb)).count();
  }
}
