package com.howtodoinjava.hibernate.emf;

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import org.hibernate.jpa.HibernatePersistenceProvider;

/**
 * The information a container (Spring, a Jakarta EE server) passes to the provider instead of a
 * persistence.xml file. Frameworks build this object; applications rarely write one.
 */
@SuppressWarnings("deprecation")   // PersistenceUnitInfo still returns the spi transaction type
public class CoffeeShopUnitInfo implements PersistenceUnitInfo {

  private final DataSource dataSource;
  private final Properties properties = new Properties();

  public CoffeeShopUnitInfo(DataSource dataSource) {
    this.dataSource = dataSource;
    properties.put("jakarta.persistence.schema-generation.database.action", "drop-and-create");
    properties.put("hibernate.show_sql", "true");
  }

  @Override
  public String getPersistenceUnitName() {
    return "coffee-shop-container";
  }

  @Override
  public String getPersistenceProviderClassName() {
    return HibernatePersistenceProvider.class.getName();
  }

  @Override
  public String getScopeAnnotationName() {
    return null;
  }

  @Override
  public List<String> getQualifierAnnotationNames() {
    return List.of();
  }

  @Override
  public PersistenceUnitTransactionType getTransactionType() {
    return PersistenceUnitTransactionType.RESOURCE_LOCAL;
  }

  @Override
  public DataSource getJtaDataSource() {
    return null;
  }

  @Override
  public DataSource getNonJtaDataSource() {
    return dataSource;
  }

  @Override
  public List<String> getMappingFileNames() {
    return List.of();
  }

  @Override
  public List<URL> getJarFileUrls() {
    return List.of();
  }

  @Override
  public URL getPersistenceUnitRootUrl() {
    return null;
  }

  @Override
  public List<String> getManagedClassNames() {
    return List.of(Drink.class.getName());
  }

  @Override
  public boolean excludeUnlistedClasses() {
    return true;
  }

  @Override
  public SharedCacheMode getSharedCacheMode() {
    return SharedCacheMode.UNSPECIFIED;
  }

  @Override
  public ValidationMode getValidationMode() {
    return ValidationMode.AUTO;
  }

  @Override
  public Properties getProperties() {
    return properties;
  }

  @Override
  public String getPersistenceXMLSchemaVersion() {
    return "3.2";
  }

  @Override
  public ClassLoader getClassLoader() {
    return Thread.currentThread().getContextClassLoader();
  }

  @Override
  public void addTransformer(ClassTransformer transformer) {
    // no bytecode enhancement at runtime
  }

  @Override
  public ClassLoader getNewTempClassLoader() {
    return null;
  }
}
