package com.howtodoinjava.hibernate.interceptors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.type.Type;

/**
 * Session-scoped interceptor: it keeps the pending audit rows of ONE session,
 * so a new instance is needed for every session.
 */
public class AuditInterceptor implements Interceptor {

  private final List<AuditLog> pending = new ArrayList<>();
  private Session session;

  public static Session openSession(SessionFactory sessionFactory) {
    AuditInterceptor audit = new AuditInterceptor();
    Session session = sessionFactory.withOptions().interceptor(audit).openSession();
    audit.session = session;
    return session;
  }

  @Override
  public boolean onPersist(Object entity, Object id, Object[] state,
                           String[] propertyNames, Type[] types) {
    if (!(entity instanceof Appointment)) {
      return false;
    }
    int status = indexOf(propertyNames, "status");
    boolean changed = false;
    if (state[status] == null) {
      state[status] = "booked";     // change the state array, not the entity
      changed = true;
    }
    pending.add(new AuditLog("Appointment", (Long) id, "insert", null, null, null));
    return changed;
  }

  @Override
  public boolean onFlushDirty(Object entity, Object id, Object[] currentState,
                              Object[] previousState, String[] propertyNames, Type[] types) {
    if (!(entity instanceof Appointment)) {
      return false;
    }
    for (int i = 0; i < propertyNames.length; i++) {
      if (!Objects.equals(previousState[i], currentState[i])) {
        pending.add(new AuditLog("Appointment", (Long) id, "update", propertyNames[i],
            String.valueOf(previousState[i]), String.valueOf(currentState[i])));
      }
    }
    return false;
  }

  @Override
  public void onRemove(Object entity, Object id, Object[] state,
                       String[] propertyNames, Type[] types) {
    if (entity instanceof Appointment) {
      pending.add(new AuditLog("Appointment", (Long) id, "delete", null, null, null));
    }
  }

  @Override
  public void postFlush(Iterator<Object> entities) {
    if (pending.isEmpty()) {
      return;
    }
    // same JDBC connection, so the audit rows commit or roll back with the change
    try (StatelessSession writer = session.statelessWithOptions()
        .connection()
        .noInterceptor()
        .open()) {
      pending.forEach(writer::insert);
    }
    pending.clear();
  }

  private static int indexOf(String[] propertyNames, String name) {
    for (int i = 0; i < propertyNames.length; i++) {
      if (propertyNames[i].equals(name)) {
        return i;
      }
    }
    throw new IllegalArgumentException(name);
  }
}
