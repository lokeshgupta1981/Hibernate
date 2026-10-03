package com.howtodoinjava.hibernate.namedprocedure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

// Spring Data JPA: calls the @NamedStoredProcedureQuery by its name
public interface StockItemRepository extends JpaRepository<StockItem, Long> {

  @Procedure(name = "StockItem.countUnits")
  Integer countUnits(@Param("p_warehouse") String warehouse);
}
