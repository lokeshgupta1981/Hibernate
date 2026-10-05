package com.howtodoinjava.hibernate.naturalid;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/** Records every SQL statement Hibernate prepares, so the tests can check it. */
public class SqlLog implements StatementInspector {

  private final List<String> statements = new CopyOnWriteArrayList<>();

  @Override
  public String inspect(String sql) {
    statements.add(sql);
    return sql;
  }

  public List<String> statements() {
    return List.copyOf(statements);
  }

  public void clear() {
    statements.clear();
  }
}
