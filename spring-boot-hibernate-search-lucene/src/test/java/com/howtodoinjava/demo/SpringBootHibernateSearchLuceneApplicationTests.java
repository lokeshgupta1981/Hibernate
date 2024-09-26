package com.howtodoinjava.demo;

import com.howtodoinjava.demo.config.JpaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class SpringBootHibernateSearchLuceneApplicationTests {

  @Test
  void contextLoads() {
  }

}
