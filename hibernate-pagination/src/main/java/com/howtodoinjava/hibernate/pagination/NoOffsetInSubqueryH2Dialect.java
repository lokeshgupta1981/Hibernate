package com.howtodoinjava.hibernate.pagination;

import org.hibernate.dialect.H2Dialect;

/**
 * H2 dialect that reports no support for OFFSET/FETCH in subqueries, as on Sybase ASE.
 * Hibernate then pages a collection fetch in memory, which is what Hibernate 7.3 and older always did.
 */
public class NoOffsetInSubqueryH2Dialect extends H2Dialect {

  @Override
  public boolean supportsOffsetInSubquery() {
    return false;
  }
}
