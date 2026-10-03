package com.howtodoinjava.hibernate.immutable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

// A record cannot be an entity: the test shows the bootstrap error
@Entity
public record LedgerEntryRecord(@Id Long id, String description) {
}
