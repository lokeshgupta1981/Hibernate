package com.howtodoinjava.hibernate.namedquery;

import jakarta.persistence.NamedQuery;

/** Compares a BigDecimal attribute with a string literal. */
@NamedQuery(name = "Room.findCheap", query = "select r from Room r where r.nightlyRate = 'cheap'")
public class BadTypeQueries {
}
