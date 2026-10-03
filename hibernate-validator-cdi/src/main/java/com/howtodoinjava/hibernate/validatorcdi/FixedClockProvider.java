package com.howtodoinjava.hibernate.validatorcdi;

import jakarta.validation.ClockProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Registered in META-INF/validation.xml. @PastOrPresent compares against this clock,
 * so "today" is the 2026 filing deadline in every run.
 */
public class FixedClockProvider implements ClockProvider {

  @Override
  public Clock getClock() {
    return Clock.fixed(Instant.parse("2026-04-15T10:00:00Z"), ZoneOffset.UTC);
  }
}
