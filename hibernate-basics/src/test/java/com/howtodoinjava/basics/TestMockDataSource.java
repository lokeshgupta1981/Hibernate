package com.howtodoinjava.basics;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;

public class TestMockDataSource {

  private static InitialContext initContext;

  @BeforeAll
  public static void setup() throws Exception {
    initContext = new InitialContext();
  }

  @Test
  public void whenMockJndiDataSource_thenReturnJndiDataSource() throws Exception {
    Context envContext = (Context) this.initContext.lookup("java:/comp/env");
    DataSource ds = (DataSource) envContext.lookup("datasource/ds");

    Assertions.assertEquals(
        "org.h2.Driver::::jdbc:h2:mem:testdb::::sa", ds.toString());

    Connection conn = ds.getConnection();
    Assertions.assertNotNull(conn);
  }

  @AfterAll
  public static void tearDown() throws Exception {
    initContext.close();
  }
}
