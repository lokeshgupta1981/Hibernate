package com.howtodoinjava.hibernate.validatorcdi;

import jakarta.persistence.EntityManagerFactory;
import jakarta.validation.ValidatorFactory;
import org.hibernate.cfg.ValidationSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  private Database() {
  }

  /**
   * @param validatorFactory the CDI-managed factory, or null to let Hibernate build its own
   */
  public static EntityManagerFactory create(String name, ValidatorFactory validatorFactory) {
    HibernatePersistenceConfiguration config = new HibernatePersistenceConfiguration(name)
        .managedClass(TaxFiling.class)
        .jdbcUrl("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(true, false, false);
    if (validatorFactory != null) {
      config.property(ValidationSettings.JAKARTA_VALIDATION_FACTORY, validatorFactory);
    }
    return config.createEntityManagerFactory();
  }
}
