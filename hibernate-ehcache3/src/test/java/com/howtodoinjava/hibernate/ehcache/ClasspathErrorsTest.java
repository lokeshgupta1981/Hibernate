package com.howtodoinjava.hibernate.ehcache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

/**
 * Starts Hibernate in a separate JVM with a changed classpath, to show the errors of a missing or
 * wrong jar: no hibernate-jcache, or the ehcache jar without the jakarta classifier.
 */
class ClasspathErrorsTest {

  private static List<String> testClasspath() {
    String cp = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
    return new ArrayList<>(List.of(cp.split(File.pathSeparator)));
  }

  /** Runs StartCheck (Database.create) in a new JVM and returns what it printed. */
  private static String startWith(UnaryOperator<List<String>> change) throws Exception {
    String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    String classpath = String.join(File.pathSeparator, change.apply(testClasspath()));
    Process process = new ProcessBuilder(java, "-cp", classpath, StartCheck.class.getName())
        .redirectErrorStream(true)
        .start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    assertTrue(process.waitFor(60, TimeUnit.SECONDS));
    return output;
  }

  @Test
  void unchangedClasspathStarts() throws Exception {
    assertTrue(startWith(cp -> cp).contains("started"));
  }

  @Test
  void withoutHibernateJcacheTheNameJcacheIsUnknown() throws Exception {
    String output = startWith(cp -> {
      cp.removeIf(e -> e.contains("hibernate-jcache"));
      return cp;
    });
    assertTrue(output.contains("org.hibernate.service.spi.ServiceException: Unable to create requested service"
        + " [org.hibernate.cache.spi.RegionFactory] due to: Unable to resolve name [jcache] as strategy"
        + " [org.hibernate.cache.spi.RegionFactory]"), output);
    assertTrue(output.contains("java.lang.ClassNotFoundException: Could not load requested class: jcache"), output);
  }

  @Test
  void ehcacheWithoutJakartaClassifierCannotReadEhcacheXml() throws Exception {
    File plainJar = new File("target/plain-ehcache/ehcache-3.12.0.jar");
    assertTrue(plainJar.isFile(), "run mvn test: the build copies the plain jar first");
    String output = startWith(cp -> {
      cp.removeIf(e -> e.endsWith("ehcache-3.12.0-jakarta.jar"));
      cp.add(plainJar.getAbsolutePath());
      return cp;
    });
    assertTrue(output.contains("java.lang.NoClassDefFoundError: javax/xml/bind/ValidationEventHandler"), output);
    assertEquals(-1, output.indexOf("started"));
  }
}
