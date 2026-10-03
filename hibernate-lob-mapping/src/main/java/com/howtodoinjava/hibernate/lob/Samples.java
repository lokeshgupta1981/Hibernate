package com.howtodoinjava.hibernate.lob;

import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import org.hibernate.Hibernate;
import org.hibernate.ReadOnlyMode;

/** Test data and the heap measurement used by the demo and the tests. */
public final class Samples {

  private Samples() {
  }

  /** Creates target/portfolio.zip with the given number of pseudo-random bytes. */
  public static Path portfolio(int bytes) {
    try {
      Path path = Path.of("target/portfolio.zip");
      Files.createDirectories(path.getParent());
      byte[] data = new byte[bytes];
      new Random(42).nextBytes(data);
      Files.write(path, data);
      return path;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static long usedHeap() {
    Runtime rt = Runtime.getRuntime();
    for (int i = 0; i < 3; i++) {
      System.gc();
    }
    return rt.totalMemory() - rt.freeMemory();
  }

  /** Returns {byteArrayDelta, blobDelta, readOnlyByteArrayDelta} in bytes: heap retained while one loaded entity is referenced. */
  public static long[] heapDeltas(EntityManagerFactory emf, Path file) {
    try {
      long size = Files.size(file);
      Long appId = saveAsByteArray(emf, file);
      Long fileId = emf.callInTransaction(em -> {
        try (InputStream in = Files.newInputStream(file)) {
          ApplicationFile f = new ApplicationFile("heap-test.zip", size);
          f.setContent(Hibernate.getLobHelper().createBlob(in, size));
          em.persist(f);
          em.flush();
          return f.getId();
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });

      long[] result = new long[3];
      emf.runInTransaction(em -> {
        long before = usedHeap();
        JobApplication app = em.find(JobApplication.class, appId);
        long after = usedHeap();
        result[0] = after - before + 0 * app.getResume().length;
      });
      emf.runInTransaction(em -> {
        long before = usedHeap();
        JobApplication app = em.find(JobApplication.class, appId, ReadOnlyMode.READ_ONLY);
        long after = usedHeap();
        result[2] = after - before + 0 * app.getResume().length;
      });
      emf.runInTransaction(em -> {
        long before = usedHeap();
        ApplicationFile f = em.find(ApplicationFile.class, fileId);
        Object blob = f.getContent();
        long after = usedHeap();
        result[1] = after - before + 0 * blob.hashCode();
      });
      return result;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Long saveAsByteArray(EntityManagerFactory emf, Path file) throws IOException {
    byte[] data = Files.readAllBytes(file);
    return emf.callInTransaction(em -> {
      JobApplication app = new JobApplication("Heap test");
      app.setResume(data);
      em.persist(app);
      return app.getId();
    });
  }

  public static String heapReport(EntityManagerFactory emf, Path file) {
    long[] d = heapDeltas(emf, file);
    long mb = 1024 * 1024;
    return "byte[] entity: ~" + d[0] / mb + " MB, byte[] read-only: ~" + d[2] / mb + " MB, Blob entity: ~" + d[1] / mb + " MB";
  }
}
