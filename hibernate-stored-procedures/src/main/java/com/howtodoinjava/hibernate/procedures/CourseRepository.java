package com.howtodoinjava.hibernate.procedures;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

// Spring Data JPA: a repository method that calls the procedure
public interface CourseRepository extends JpaRepository<Course, Long> {

  @Procedure(procedureName = "count_courses_by_level", outputParameterName = "p_total")
  Integer countByLevel(@Param("p_level") String level);
}
