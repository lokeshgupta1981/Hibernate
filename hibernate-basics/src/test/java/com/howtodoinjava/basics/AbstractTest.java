package com.howtodoinjava.basics;

import com.mysql.cj.jdbc.MysqlDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractTest {

  private SessionFactory sessionFactory = null;

  private static DataSource getDataSource() {

    // Create DataSource
    MysqlDataSource ds = new MysqlDataSource();
    ds.setURL("jdbc:mysql://localhost/testdb");
    ds.setUser("root");
    ds.setPassword("password");

    // Create ProxyDataSource
    ProxyDataSource dataSource = ProxyDataSourceBuilder.create(ds)
        .asJson()
        .countQuery()
        .logQueryToSysOut()
        .multiline()
        .build();

    return dataSource;
  }

  public SessionFactory buildSessionFactory() {
    if (sessionFactory == null) {
      try {


        Map<String, Object> settings = new HashMap<>();
        settings.put(Environment.DATASOURCE, getDataSource());
        settings.put(Environment.DRIVER, "com.mysql.cj.jdbc.Driver");
        settings.put(Environment.DIALECT, "org.hibernate.dialect" +
            ".MySQL8Dialect");
        settings.put(Environment.HBM2DDL_AUTO, "update");
        settings.put("hibernate.jdbc.batch_size", 50);
        settings.put("hibernate.order_inserts", true);
        /*settings.put("hibernate.order_updates", true);
        settings.put("hibernate.batch_versioned_data", "true");*/

        StandardServiceRegistry standardRegistry
            = new StandardServiceRegistryBuilder()
            .applySettings(settings)
            .build();

        MetadataSources metadataSources = new MetadataSources(standardRegistry);

        for (Class clazz : getEntities()) {
          metadataSources.addAnnotatedClass(clazz);
        }

        Metadata metadata = metadataSources.getMetadataBuilder().build();

        sessionFactory = metadata
            .getSessionFactoryBuilder().build();

      } catch (Throwable ex) {
        throw new ExceptionInInitializerError(ex);
      }
    }
    return sessionFactory;
  }

  public abstract List<Class<?>> getEntities();

  public void doInTransaction(HibernateTransactionConsumer callable) {
    Session session = null;
    Transaction txn = null;
    try {
      session = buildSessionFactory().openSession();
      callable.beforeTransactionCompletion();
      txn = session.beginTransaction();

      callable.accept(session);
      txn.commit();
    } catch (RuntimeException e) {
      if (txn != null && txn.isActive()) txn.rollback();
      throw e;
    } finally {
      callable.afterTransactionCompletion();
      if (session != null) {
        session.close();
      }
    }
  }
}
