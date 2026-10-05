package com.howtodoinjava.hibernate.interceptors;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.hibernate.Interceptor;
import org.hibernate.type.Type;

/** SessionFactory-scoped: one instance for all sessions, so no per-session fields. */
public class LoggingInterceptor implements Interceptor {

  private final List<String> log = new CopyOnWriteArrayList<>();

  @Override
  public boolean onFlushDirty(Object entity, Object id, Object[] currentState,
                              Object[] previousState, String[] propertyNames, Type[] types) {
    String line = entity.getClass().getSimpleName() + "#" + id + " changed from "
        + Arrays.toString(previousState) + " to " + Arrays.toString(currentState);
    log.add(line);
    System.out.println(line);
    return false;
  }

  public List<String> log() {
    return log;
  }
}
