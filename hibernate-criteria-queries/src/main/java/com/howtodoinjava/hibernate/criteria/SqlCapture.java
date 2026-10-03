package com.howtodoinjava.hibernate.criteria;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/** Records every SQL statement Hibernate sends, so tests can assert it. */
public class SqlCapture implements StatementInspector {

  private final List<String> statements = new ArrayList<>();

  @Override
  public String inspect(String sql) {
    statements.add(sql);
    return sql;
  }

  public List<String> statements() {
    return statements;
  }

  public String last() {
    return statements.getLast();
  }

  public void clear() {
    statements.clear();
  }
}
