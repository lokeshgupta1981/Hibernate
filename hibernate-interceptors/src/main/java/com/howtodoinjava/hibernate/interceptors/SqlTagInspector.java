package com.howtodoinjava.hibernate.interceptors;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/** Shared by every session, so it is stateless except for a thread-safe list. */
public class SqlTagInspector implements StatementInspector {

  private final List<String> statements = new CopyOnWriteArrayList<>();

  @Override
  public String inspect(String sql) {
    String tagged = "/* clinic-app */ " + sql;
    statements.add(tagged);
    return tagged;
  }

  public List<String> statements() {
    return statements;
  }
}
