package com.howtodoinjava.hibernate.interceptors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;

/** Records the order in which Hibernate calls the interceptor. */
public class TracingInterceptor implements Interceptor {

  private final List<String> calls = new ArrayList<>();

  public List<String> calls() {
    return calls;
  }

  private void record(String call) {
    calls.add(call);
    System.out.println("  interceptor: " + call);
  }

  @Override
  public void afterTransactionBegin(Transaction tx) {
    record("afterTransactionBegin");
  }

  @Override
  public boolean onLoad(Object entity, Object id, Object[] state, String[] names, Type[] types) {
    record("onLoad");
    return false;
  }

  @Override
  public boolean onPersist(Object entity, Object id, Object[] state, String[] names, Type[] types) {
    record("onPersist");
    return false;
  }

  @Override
  public void onRemove(Object entity, Object id, Object[] state, String[] names, Type[] types) {
    record("onRemove");
  }

  @Override
  public void preFlush(Iterator<Object> entities) {
    record("preFlush");
  }

  @Override
  public int[] findDirty(Object entity, Object id, Object[] currentState, Object[] previousState,
                         String[] names, Type[] types) {
    record("findDirty");
    return null;
  }

  @Override
  public boolean onFlushDirty(Object entity, Object id, Object[] currentState,
                              Object[] previousState, String[] names, Type[] types) {
    record("onFlushDirty");
    return false;
  }

  @Override
  public void postFlush(Iterator<Object> entities) {
    record("postFlush");
  }

  @Override
  public void beforeTransactionCompletion(Transaction tx) {
    record("beforeTransactionCompletion");
  }

  @Override
  public void afterTransactionCompletion(Transaction tx) {
    record("afterTransactionCompletion");
  }
}
