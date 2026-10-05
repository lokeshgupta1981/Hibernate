package com.howtodoinjava.hibernate.persister.boot.scanmissing;

import org.springframework.boot.autoconfigure.SpringBootApplication;

// Cause 5: Spring Boot scans entities only in this package and below.
// FerryRoute lives in com.howtodoinjava.hibernate.persister, outside of it.
@SpringBootApplication
public class FerryBootApp {
}
