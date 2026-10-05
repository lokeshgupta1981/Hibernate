package com.howtodoinjava.hibernate.springconfig;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

  List<Showtime> findByScreenOrderByStartsAt(int screen);
}
