package com.howtodoinjava.hibernate.namedquery;

import java.util.List;
import org.hibernate.annotations.processing.HQL;

/**
 * Query methods checked at compile time. Hibernate Processor generates HotelQueries_.
 */
public interface HotelQueries {

  @HQL("where type = :type and available = true order by number")
  List<Room> findAvailable(String type);

  @HQL("select count(*) from Booking where room.number = :number")
  long countBookings(Integer number);
}
