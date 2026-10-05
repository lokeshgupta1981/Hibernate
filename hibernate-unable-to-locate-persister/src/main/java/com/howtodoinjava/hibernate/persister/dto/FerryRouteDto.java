package com.howtodoinjava.hibernate.persister.dto;

// Cause 4: a DTO is not an entity, even when it has the same fields
public record FerryRouteDto(String fromPort, String toPort, int durationMinutes) {
}
