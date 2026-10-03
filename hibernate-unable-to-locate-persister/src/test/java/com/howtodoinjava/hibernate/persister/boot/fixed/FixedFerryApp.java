package com.howtodoinjava.hibernate.persister.boot.fixed;

import com.howtodoinjava.hibernate.persister.FerryRoute;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

// Fix: tell Spring Boot where the entities are
@SpringBootApplication
@EntityScan(basePackageClasses = FerryRoute.class)
public class FixedFerryApp {
}
