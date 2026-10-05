package com.howtodoinjava.hibernate.callbacks;

import java.util.ArrayList;
import java.util.List;

/** Records every callback and every SQL statement in the order they happen. */
public final class CallbackLog {

  private static final List<String> EVENTS = new ArrayList<>();
  private static boolean print = true;

  private CallbackLog() {
  }

  public static synchronized void add(String event) {
    EVENTS.add(event);
    if (print) {
      System.out.println("  " + event);
    }
  }

  public static synchronized void sql(String sql) {
    EVENTS.add("sql: " + sql);
  }

  public static synchronized List<String> events() {
    return List.copyOf(EVENTS);
  }

  public static synchronized void clear() {
    EVENTS.clear();
  }

  public static synchronized void print(boolean on) {
    print = on;
  }
}
