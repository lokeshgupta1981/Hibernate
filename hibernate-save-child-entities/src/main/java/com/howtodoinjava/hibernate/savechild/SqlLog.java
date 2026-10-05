package com.howtodoinjava.hibernate.savechild;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/** Records every SQL statement Hibernate sends, so the tests can assert the SQL shown in the article. */
public class SqlLog implements StatementInspector {

  private final List<String> statements = new ArrayList<>();

  @Override
  public synchronized String inspect(String sql) {
    statements.add(sql);
    return sql;
  }

  public synchronized List<String> statements() {
    return List.copyOf(statements);
  }

  public synchronized void clear() {
    statements.clear();
  }
}
