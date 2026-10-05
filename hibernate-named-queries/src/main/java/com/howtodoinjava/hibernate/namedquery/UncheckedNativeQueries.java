package com.howtodoinjava.hibernate.namedquery;

import jakarta.persistence.NamedNativeQuery;

/**
 * Holds a native named query with a wrong table name (Rooms). Hibernate does not check native SQL
 * at startup, so the factory starts and the query fails only when it runs.
 */
@NamedNativeQuery(name = "Room.findFreeSql",
    query = "select * from Rooms where available = true",
    resultClass = Room.class)
public class UncheckedNativeQueries {
}
