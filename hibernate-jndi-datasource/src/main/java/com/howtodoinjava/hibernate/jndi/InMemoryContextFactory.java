package com.howtodoinjava.hibernate.jndi;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

/**
 * A JNDI provider for tests: every InitialContext reads and writes one shared map.
 * Register it with the system property java.naming.factory.initial, a jndi.properties
 * file or Hibernate's hibernate.jndi.class setting.
 */
public class InMemoryContextFactory implements InitialContextFactory {

  private static final Map<String, Object> BINDINGS = new ConcurrentHashMap<>();

  @Override
  public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
    return new InMemoryContext();
  }

  public static void clear() {
    BINDINGS.clear();
  }

  /** InitialContext(true) skips the provider search, so we only override what we need. */
  static class InMemoryContext extends InitialContext {

    InMemoryContext() throws NamingException {
      super(true);
    }

    @Override
    public Object lookup(String name) throws NamingException {
      Object value = BINDINGS.get(name);
      if (value == null) {
        throw new NameNotFoundException(name + " is not bound");
      }
      return value;
    }

    @Override
    public Object lookup(Name name) throws NamingException {
      return lookup(name.toString());
    }

    @Override
    public void bind(String name, Object value) {
      BINDINGS.put(name, value);
    }

    @Override
    public void rebind(String name, Object value) {
      BINDINGS.put(name, value);
    }

    @Override
    public void unbind(String name) {
      BINDINGS.remove(name);
    }

    @Override
    public NameParser getNameParser(String name) {
      return CompositeName::new;
    }

    @Override
    public void close() {
      // keep the bindings; Hibernate closes its InitialContext after every lookup
    }
  }
}
