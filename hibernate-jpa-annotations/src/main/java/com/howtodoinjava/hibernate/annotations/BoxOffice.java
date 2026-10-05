package com.howtodoinjava.hibernate.annotations;

import jakarta.persistence.Embeddable;

@Embeddable
public record BoxOffice(String phone, String email) {
}
