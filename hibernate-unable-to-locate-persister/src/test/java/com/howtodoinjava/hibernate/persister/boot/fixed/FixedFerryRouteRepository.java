package com.howtodoinjava.hibernate.persister.boot.fixed;

import com.howtodoinjava.hibernate.persister.FerryRoute;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FixedFerryRouteRepository extends JpaRepository<FerryRoute, Long> {
}
