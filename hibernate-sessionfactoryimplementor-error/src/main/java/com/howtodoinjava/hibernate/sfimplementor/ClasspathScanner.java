package com.howtodoinjava.hibernate.sfimplementor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Lists the classes in the jars on the class path whose bytecode refers to a given class name. */
public final class ClasspathScanner {

  private ClasspathScanner() {
  }

  /**
   * @param internalName a class name with slashes, for example "org/hibernate/engine/SessionFactoryImplementor"
   * @return "jar-file-name: class-name" for every class file that contains the name
   */
  public static List<String> findReferences(String internalName) throws IOException {
    byte[] needle = internalName.getBytes(StandardCharsets.UTF_8);
    Set<Path> jars = new LinkedHashSet<>();
    // every jar on the class path has a manifest; its URL is jar:file:/path/to/lib.jar!/META-INF/MANIFEST.MF
    Enumeration<URL> manifests = Thread.currentThread().getContextClassLoader().getResources("META-INF/MANIFEST.MF");
    while (manifests.hasMoreElements()) {
      String url = manifests.nextElement().toString();
      if (url.startsWith("jar:file:")) {
        jars.add(Path.of(URI.create(url.substring(4, url.indexOf("!/")))));
      }
    }
    List<String> hits = new ArrayList<>();
    for (Path jar : jars) {
      scanJar(jar, needle, hits);
    }
    return hits;
  }

  private static void scanJar(Path jar, byte[] needle, List<String> hits) throws IOException {
    try (JarFile file = new JarFile(jar.toFile())) {
      Enumeration<JarEntry> entries = file.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        if (entry.getName().endsWith(".class")) {
          try (InputStream in = file.getInputStream(entry)) {
            if (contains(in.readAllBytes(), needle)) {
              hits.add(jar.getFileName() + ": " + toClassName(entry.getName()));
            }
          }
        }
      }
    }
  }

  private static String toClassName(String fileName) {
    return fileName.replace(".class", "").replace('/', '.');
  }

  private static boolean contains(byte[] data, byte[] needle) {
    outer:
    for (int i = 0; i <= data.length - needle.length; i++) {
      for (int j = 0; j < needle.length; j++) {
        if (data[i + j] != needle[j]) {
          continue outer;
        }
      }
      return true;
    }
    return false;
  }
}
