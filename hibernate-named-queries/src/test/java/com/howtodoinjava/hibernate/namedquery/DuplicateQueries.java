package com.howtodoinjava.hibernate.namedquery;

import jakarta.persistence.NamedQuery;

/** Declares a query name that Room already uses. */
@NamedQuery(name = "Room.countAvailable", query = "select count(b) from Booking b")
public class DuplicateQueries {
}
