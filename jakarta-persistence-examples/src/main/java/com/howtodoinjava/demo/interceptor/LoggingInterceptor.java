package com.howtodoinjava.demo.interceptor;

import org.hibernate.Interceptor;
import org.hibernate.type.Type;

import java.util.Arrays;

public class LoggingInterceptor implements Interceptor {
  @Override
  public boolean onFlushDirty(
    Object entity,
    Object id,
    Object[] currentState,
    Object[] previousState,
    String[] propertyNames,
    Type[] types) {
    System.out.println(String.format("Entity %s#%s changed from %s to %s",
      entity.getClass().getSimpleName(),
      id,
      Arrays.toString(previousState),
      Arrays.toString(currentState)
    ));
   /* return super.onFlushDirty(entity, id, currentState,
      previousState, propertyNames, types
    );*/
    return false;
  }
}
