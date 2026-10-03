package com.howtodoinjava.hibernate.interceptors;

import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.SessionEventSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  private static HibernatePersistenceConfiguration config(String name, boolean showSql) {
    return new HibernatePersistenceConfiguration(name)
        .managedClasses(Appointment.class, AuditLog.class)
        .jdbcUrl("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false);
  }

  /** No interceptor on the factory: sessions add their own. */
  public static SessionFactory create(boolean showSql) {
    return config("clinic", showSql)
        .createEntityManagerFactory()
        .unwrap(SessionFactory.class);
  }

  /** One interceptor instance shared by every session. */
  public static SessionFactory withSharedInterceptor(Interceptor interceptor, boolean showSql) {
    return config("clinic_shared", showSql)
        .property(SessionEventSettings.INTERCEPTOR, interceptor)
        .createEntityManagerFactory()
        .unwrap(SessionFactory.class);
  }

  /** A new interceptor instance for every session. */
  public static SessionFactory withSessionScopedInterceptor(
      Class<? extends Interceptor> type, boolean showSql) {
    return config("clinic_scoped", showSql)
        .property(SessionEventSettings.SESSION_SCOPED_INTERCEPTOR, type)
        .createEntityManagerFactory()
        .unwrap(SessionFactory.class);
  }

  public static SessionFactory withStatementInspector(StatementInspector inspector, boolean showSql) {
    return config("clinic_sql", showSql)
        .statementInspector(inspector)
        .createEntityManagerFactory()
        .unwrap(SessionFactory.class);
  }
}
