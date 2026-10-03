package com.howtodoinjava.hibernate.immutable;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.resource.jdbc.spi.StatementInspector;

// Records every SQL statement Hibernate sends, so tests can check for UPDATE statements
public class SqlLog implements StatementInspector {

  private final List<String> statements = new ArrayList<>();

  @Override
  public synchronized String inspect(String sql) {
    statements.add(sql);
    return sql;
  }

  public synchronized void clear() {
    statements.clear();
  }

  public synchronized List<String> statements() {
    return List.copyOf(statements);
  }

  public synchronized List<String> updates() {
    return statements.stream().filter(s -> s.startsWith("update")).toList();
  }
}
