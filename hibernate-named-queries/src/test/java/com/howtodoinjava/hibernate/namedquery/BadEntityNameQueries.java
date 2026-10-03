package com.howtodoinjava.hibernate.namedquery;

import jakarta.persistence.NamedQuery;

/** Uses the table-like name Rooms instead of the entity name Room. */
@NamedQuery(name = "Room.findAllRooms", query = "select r from Rooms r")
public class BadEntityNameQueries {
}
