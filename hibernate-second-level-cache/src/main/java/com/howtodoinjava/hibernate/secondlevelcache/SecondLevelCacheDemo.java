package com.howtodoinjava.hibernate.secondlevelcache;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.EntityManagerFactory;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.cache.jcache.internal.JCacheAccessImpl;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.CollectionCacheEntry;
import org.hibernate.cache.spi.support.DomainDataRegionTemplate;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.stat.Statistics;

public class SecondLevelCacheDemo {

  static final String EURO_COUNTRIES =
      "select c from Country c join c.currencies cur where cur.name = :currency order by c.name";

  public static void main(String[] args) throws SQLException {
    try (EntityManagerFactory emf = Database.create(true)) {
      Statistics stats = Database.statistics(emf);

      step("1. Save four countries and three currencies");
      Long indiaId = saveCountries(emf);
      print(stats);
      System.out.println("India in cache after persist: " + emf.getCache().contains(Country.class, indiaId));

      step("2. Empty the cache and reset the statistics");
      emf.getCache().evictAll();
      stats.clear();
      System.out.println("India in cache: " + emf.getCache().contains(Country.class, indiaId));

      step("3. Session 1: find India, find it again, clear the session, find it again");
      emf.runInTransaction(em -> {
        Country first = em.find(Country.class, indiaId);
        System.out.println("find 1: " + first);
        print(stats);
        Country second = em.find(Country.class, indiaId);
        System.out.println("find 2: " + second + ", same object: " + (first == second));
        print(stats);
        em.clear();
        Country third = em.find(Country.class, indiaId);
        System.out.println("find 3 after clear(): " + third + ", same object: " + (first == third));
        print(stats);
      });

      step("4. Session 2: find India");
      emf.runInTransaction(em -> System.out.println("find: " + em.find(Country.class, indiaId)));
      print(stats);

      step("5. Session 3: read the currencies of India");
      emf.runInTransaction(em -> System.out.println("currencies: " + em.find(Country.class, indiaId).currencyNames()));
      print(stats);

      step("6. Session 4: read the currencies again");
      emf.runInTransaction(em -> System.out.println("currencies: " + em.find(Country.class, indiaId).currencyNames()));
      print(stats);

      step("7. What the regions store for India");
      dump(emf, Country.class.getName(), Country.class.getName() + "#" + indiaId);
      dump(emf, Country.class.getName() + ".currencies", Country.class.getName() + ".currencies#" + indiaId);
      dump(emf, Currency.class.getName(), Currency.class.getName() + "#1");

      step("8. Cached query, run in two sessions");
      for (int i = 0; i < 2; i++) {
        System.out.println("euro countries: " + euroCountries(emf));
        print(stats);
      }

      step("9. Update the capital of India through Hibernate");
      emf.runInTransaction(em -> em.find(Country.class, indiaId).setCapital("Delhi"));
      print(stats);
      emf.runInTransaction(em -> System.out.println("find: " + em.find(Country.class, indiaId)));
      print(stats);

      step("10. Run the cached query after the update");
      System.out.println("euro countries: " + euroCountries(emf));
      print(stats);

      step("11. Another program changes the row with plain JDBC");
      try (Connection con = DriverManager.getConnection(Database.URL, "sa", "");
           Statement st = con.createStatement()) {
        st.executeUpdate("update Country set capital = 'New Delhi' where name = 'India'");
      }
      emf.runInTransaction(em -> System.out.println("find: " + em.find(Country.class, indiaId)));
      print(stats);

      step("12. Skip the cache for one find");
      emf.runInTransaction(em ->
          System.out.println("find with BYPASS: " + em.find(Country.class, indiaId, CacheRetrieveMode.BYPASS)));
      emf.runInTransaction(em -> System.out.println("find: " + em.find(Country.class, indiaId)));
      print(stats);

      step("13. Evict India and find it again");
      emf.getCache().evict(Country.class, indiaId);
      System.out.println("India in cache: " + emf.getCache().contains(Country.class, indiaId));
      emf.runInTransaction(em -> System.out.println("find: " + em.find(Country.class, indiaId)));
      print(stats);

      step("14. Change an @Immutable READ_ONLY currency");
      emf.runInTransaction(em -> em.find(Currency.class, 1L).setName("Indian Rupee"));
      emf.runInTransaction(em -> System.out.println("currency: " + em.find(Currency.class, 1L)));
      print(stats);

      step("15. Statistics of the Country region");
      var region = stats.getDomainDataRegionStatistics(Country.class.getName());
      System.out.println("Country region: hits=" + region.getHitCount() + ", misses=" + region.getMissCount()
          + ", puts=" + region.getPutCount());
      var queries = stats.getQueryStatistics(EURO_COUNTRIES);
      System.out.println("Euro query: executions=" + queries.getExecutionCount()
          + ", cache hits=" + queries.getCacheHitCount() + ", cache misses=" + queries.getCacheMissCount());
    }
  }

  static Long saveCountries(EntityManagerFactory emf) {
    return emf.callInTransaction(em -> {
      Currency rupee = new Currency("Rupee");
      Currency euro = new Currency("Euro");
      Currency yen = new Currency("Yen");
      em.persist(rupee);
      em.persist(euro);
      em.persist(yen);

      Country india = new Country("India", "New Delhi");
      india.addCurrency(rupee);
      Country germany = new Country("Germany", "Berlin");
      germany.addCurrency(euro);
      Country france = new Country("France", "Paris");
      france.addCurrency(euro);
      Country japan = new Country("Japan", "Tokyo");
      japan.addCurrency(yen);
      em.persist(india);
      em.persist(germany);
      em.persist(france);
      em.persist(japan);
      return india.getId();
    });
  }

  static List<Country> euroCountries(EntityManagerFactory emf) {
    return emf.callInTransaction(em -> em.createQuery(EURO_COUNTRIES, Country.class)
        .setParameter("currency", "Euro")
        .setHint(HibernateHints.HINT_CACHEABLE, true)
        .getResultList());
  }

  /** Prints the raw entry that Ehcache holds for one key of a region. */
  static String dump(EntityManagerFactory emf, String regionName, String key) {
    var region = emf.unwrap(SessionFactoryImplementor.class).getCache().getRegion(regionName);
    var storage = (JCacheAccessImpl) ((DomainDataRegionTemplate) region).getCacheStorageAccess();
    StringBuilder out = new StringBuilder();
    storage.getUnderlyingCache().forEach(entry -> {
      if (!entry.getKey().toString().equals(key)) {
        return;
      }
      Object value = unwrapItem(entry.getValue());
      String state = switch (value) {
        case CacheEntry ce -> "CacheEntry " + Arrays.toString(ce.getDisassembledState());
        case CollectionCacheEntry cce -> "CollectionCacheEntry " + Arrays.toString((Object[]) cce.getState());
        default -> String.valueOf(value);
      };
      out.append(key).append(" -> ").append(state);
    });
    System.out.println(out);
    return out.toString();
  }

  private static Object unwrapItem(Object value) {
    try {
      // READ_WRITE regions wrap the entry in an Item that also holds a version and a timestamp
      return value.getClass().getSimpleName().equals("Item")
          ? value.getClass().getMethod("getValue").invoke(value)
          : value;
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void print(Statistics stats) {
    System.out.println("L2 hits=" + stats.getSecondLevelCacheHitCount()
        + ", misses=" + stats.getSecondLevelCacheMissCount()
        + ", puts=" + stats.getSecondLevelCachePutCount()
        + " | query cache hits=" + stats.getQueryCacheHitCount()
        + ", misses=" + stats.getQueryCacheMissCount()
        + " | statements=" + stats.getPrepareStatementCount());
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
  }
}
