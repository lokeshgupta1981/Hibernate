package com.howtodoinjava.hibernate.c3p0;

import static org.assertj.core.api.Assertions.assertThat;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.sql.Connection;
import java.time.Duration;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4 side: c3p0 as the application's DataSource. No web server is started;
 * ApplicationContextRunner runs the DataSource auto-configuration only.
 */
class SpringBootC3p0DataSourceTest {

  private final ApplicationContextRunner runner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
      .withPropertyValues(
          "spring.datasource.url=jdbc:h2:mem:garage-boot;DB_CLOSE_DELAY=-1",
          "spring.datasource.username=sa",
          "spring.datasource.password=");

  @Configuration(proxyBeanMethods = false)
  static class C3p0DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.c3p0")
    ComboPooledDataSource dataSource(DataSourceProperties properties) {
      return properties.initializeDataSourceBuilder()
          .type(ComboPooledDataSource.class)
          .build();
    }
  }

  @Test
  void typePropertySelectsC3p0() {
    runner.withPropertyValues("spring.datasource.type=com.mchange.v2.c3p0.ComboPooledDataSource")
        .run(context -> {
          DataSource dataSource = context.getBean(DataSource.class);
          assertThat(dataSource).isInstanceOf(ComboPooledDataSource.class);
          ComboPooledDataSource c3p0 = (ComboPooledDataSource) dataSource;
          assertThat(c3p0.getJdbcUrl()).isEqualTo("jdbc:h2:mem:garage-boot;DB_CLOSE_DELAY=-1");
          assertThat(c3p0.getMaxPoolSize()).isEqualTo(15);   // c3p0 default, pool settings are not bound
        });
  }

  @Test
  void customBeanBindsC3p0Settings() {
    runner.withUserConfiguration(C3p0DataSourceConfig.class)
        .withPropertyValues(
            "spring.datasource.c3p0.initial-pool-size=5",
            "spring.datasource.c3p0.min-pool-size=5",
            "spring.datasource.c3p0.max-pool-size=20",
            "spring.datasource.c3p0.acquire-increment=5",
            "spring.datasource.c3p0.max-idle-time=300",
            "spring.datasource.c3p0.idle-connection-test-period=30",
            "spring.datasource.c3p0.test-connection-on-checkin=true")
        .run(context -> {
          ComboPooledDataSource c3p0 = context.getBean(ComboPooledDataSource.class);
          assertThat(c3p0.getJdbcUrl()).isEqualTo("jdbc:h2:mem:garage-boot;DB_CLOSE_DELAY=-1");
          assertThat(c3p0.getUser()).isEqualTo("sa");
          assertThat(c3p0.getMinPoolSize()).isEqualTo(5);
          assertThat(c3p0.getMaxPoolSize()).isEqualTo(20);
          assertThat(c3p0.getAcquireIncrement()).isEqualTo(5);
          assertThat(c3p0.getMaxIdleTime()).isEqualTo(300);
          assertThat(c3p0.getIdleConnectionTestPeriod()).isEqualTo(30);
          assertThat(c3p0.isTestConnectionOnCheckin()).isTrue();
          try (Connection connection = c3p0.getConnection()) {
            assertThat(connection.isValid(1)).isTrue();
          }
          assertThat(Database.await(() -> Database.total(c3p0) == 5, Duration.ofSeconds(5))).isTrue();
          assertThat(context.getBeansOfType(DataSource.class)).hasSize(1);
        });
  }
}
