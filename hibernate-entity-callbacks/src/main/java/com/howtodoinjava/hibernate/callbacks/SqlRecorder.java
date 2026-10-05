package com.howtodoinjava.hibernate.callbacks;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/** Adds each SQL statement to the CallbackLog so tests can check the order. */
public class SqlRecorder implements StatementInspector {

  @Override
  public String inspect(String sql) {
    CallbackLog.sql(sql);
    return sql;
  }
}
