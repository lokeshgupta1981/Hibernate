package com.howtodoinjava.hibernate.namedquery;

import jakarta.persistence.NamedQuery;

/**
 * Holds a named query with a typo in an attribute name. Registering this class makes the
 * EntityManagerFactory fail at startup. It is used only by the demo and the tests.
 */
@NamedQuery(name = "Room.findFree",
    query = "select r from Room r where r.availble = true")
public class BrokenQueries {
}
