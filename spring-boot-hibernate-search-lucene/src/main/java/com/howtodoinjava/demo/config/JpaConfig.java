package com.howtodoinjava.demo.config;

import com.howtodoinjava.demo.repository.core.BaseRepositoryImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.howtodoinjava.demo.repository",
    repositoryBaseClass = BaseRepositoryImpl.class)
public class JpaConfig {
}
