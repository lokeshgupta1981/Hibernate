package com.howtodoinjava.basics.entity.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.CallbackException;
import org.hibernate.Interceptor;
import org.hibernate.type.Type;

import java.util.Arrays;

@Slf4j
public class AuditInterceptor implements Interceptor {

  @Override
  public boolean onFlushDirty(Object entity,
                              Object id,
                              Object[] currentState,
                              Object[] previousState,
                              String[] propertyNames,
                              Type[] types) throws CallbackException {

    if (log.isDebugEnabled()) {
      log.debug("********************AUDIT INFO START*******************");
      log.debug("Entity Name    :: " + entity.getClass());
      log.debug("Previous state :: " + Arrays.deepToString(previousState));
      log.debug("Current  state :: " + Arrays.deepToString(currentState));
      log.debug("propertyNames  :: " + Arrays.deepToString(propertyNames));
      log.debug("********************AUDIT INFO END*******************");
    }

    return Interceptor.super.onFlushDirty(entity,
      id,
      currentState,
      previousState,
      propertyNames,
      types);
  }
}
