package com.howtodoinjava.hibernate.nativeselect;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.util.List;
import org.hibernate.Session;

public class NativeSelectDemo {

  public static void main(String[] args) {
    try (EntityManagerFactory emf = Database.create(true)) {
      Database.insertSampleData(emf);

      step("1. Native query returning managed Produce entities");
      emf.runInTransaction(em -> {
        @SuppressWarnings("unchecked")
        List<Produce> fruit = em.createNativeQuery(
                "select * from Produce where category = ?1 order by pricePerKg", Produce.class)
            .setParameter(1, "fruit")
            .getResultList();
        print(fruit + ", managed = " + em.contains(fruit.get(0)));
      });

      step("1b. The farm of a native-query entity loads on first access");
      emf.runInTransaction(em -> {
        Produce apples = (Produce) em.createNativeQuery(
                "select * from Produce where name = 'Apples'", Produce.class)
            .getSingleResult();
        print("farm loaded = " + org.hibernate.Hibernate.isInitialized(apples.getFarm()));
        print("farm = " + apples.getFarm().getName());
      });

      step("2. JDBC-style ? and named parameters");
      emf.runInTransaction(em -> {
        print(em.createNativeQuery(
                "select * from Produce where category = ? and pricePerKg < ?", Produce.class)
            .setParameter(1, "fruit")
            .setParameter(2, new BigDecimal("3.00"))
            .getResultList());
        print(em.createNativeQuery(
                "select * from Produce where pricePerKg between :min and :max order by name", Produce.class)
            .setParameter("min", new BigDecimal("1.00"))
            .setParameter("max", new BigDecimal("3.00"))
            .getResultList());
        print(em.createNativeQuery(
                "select * from Produce where name in (:names) order by name", Produce.class)
            .setParameter("names", List.of("Pears", "Carrots"))
            .getResultList());
      });

      step("3. @NamedNativeQuery with resultClass");
      emf.runInTransaction(em -> print(em.createNamedQuery("Produce.findByCategory", Produce.class)
          .setParameter("category", "vegetable")
          .getResultList()));

      step("4. Scalar values: one column, Object[], Tuple");
      emf.runInTransaction(em -> {
        print(em.createNativeQuery("select name from Produce order by name").getResultList());
        Object count = em.createNativeQuery("select count(*) from Produce").getSingleResult();
        print(count + " " + count.getClass().getSimpleName());
        Object max = em.createNativeQuery("select max(pricePerKg) from Produce").getSingleResult();
        print(max + " " + max.getClass().getSimpleName());

        Object[] row = (Object[]) em.createNativeQuery(
                "select id, name, pricePerKg, harvestedOn, farm_id from Produce where name = 'Apples'")
            .getSingleResult();
        for (Object value : row) {
          print("  " + value + " -> " + value.getClass().getName());
        }

        Tuple tuple = (Tuple) em.createNativeQuery(
                "select name, pricePerKg as price from Produce where name = 'Apples'", Tuple.class)
            .getSingleResult();
        print(tuple.get("name") + " " + tuple.get("price", BigDecimal.class)
            + ", aliases " + tuple.getElements().stream().map(e -> e.getAlias()).toList());
      });

      step("5. Rows to a record");
      emf.runInTransaction(em -> {
        print(em.createNativeQuery(
                "select name, pricePerKg from Produce where category = 'vegetable' order by pricePerKg",
                ProducePrice.class)
            .getResultList());
        print(em.createNamedQuery("Produce.categoryPrices", CategoryPrice.class).getResultList());
        List<ProducePrice> cheap = em.unwrap(Session.class)
            .createNativeQuery("select name, pricePerKg from Produce where pricePerKg < 2", Object[].class)
            .setTupleTransformer((tuple, aliases) -> new ProducePrice((String) tuple[0], (BigDecimal) tuple[1]))
            .getResultList();
        print(cheap);
      });

      step("6. Two entities per row with @EntityResult");
      emf.runInTransaction(em -> {
        List<Object[]> rows = em.createNamedQuery("Produce.withFarmByRegion", Object[].class)
            .setParameter("region", "North")
            .getResultList();
        for (Object[] r : rows) {
          print("  " + r[0] + " | " + r[1]);
        }
      });
      emf.runInTransaction(em -> {
        List<Object[]> rows = em.unwrap(Session.class)
            .createNativeQuery("select {p.*}, {f.*} from Produce p join Farm f on f.id = p.farm_id "
                + "where f.region = :region order by p.name", Object[].class)
            .addEntity("p", Produce.class)
            .addEntity("f", Farm.class)
            .setParameter("region", "South")
            .getResultList();
        for (Object[] r : rows) {
          print("  " + r[0] + " | " + r[1]);
        }
      });

      step("6b. Jakarta Persistence 3.2 entities inside @NamedNativeQuery (ignored by Hibernate 7.4.11)");
      emf.runInTransaction(em -> {
        List<Object[]> rows = em.createNamedQuery("Produce.withFarmInline", Object[].class)
            .setParameter("region", "South")
            .getResultList();
        print(java.util.Arrays.toString(rows.get(0)));
      });

      step("7. Pagination");
      emf.runInTransaction(em -> print(em.createNativeQuery("select * from Produce order by name", Produce.class)
          .setFirstResult(2)
          .setMaxResults(2)
          .getResultList()));

      step("8. Pending changes are flushed before a native query");
      emf.runInTransaction(em -> {
        em.persist(new Produce("Plums", "fruit", "4.00", null, null));
        print("count = " + em.createNativeQuery("select count(*) from Produce").getSingleResult());
        em.getTransaction().setRollbackOnly();
      });

      step("9. A changed native-query entity is updated at commit");
      emf.runInTransaction(em -> {
        Produce pears = (Produce) em.createNativeQuery(
                "select * from Produce where name = :name", Produce.class)
            .setParameter("name", "Pears")
            .getSingleResult();
        pears.setPricePerKg(new BigDecimal("3.20"));
      });

      step("10. Errors");
      emf.runInTransaction(em -> {
        try {
          em.createNativeQuery("select * from Produce where name = 'Kiwi'", Produce.class).getSingleResult();
        } catch (RuntimeException e) {
          print(e.getClass().getName() + ": " + e.getMessage());
        }
      });
      emf.runInTransaction(em -> {
        try {
          em.createNativeQuery("select id, name from Produce", Produce.class).getResultList();
        } catch (RuntimeException e) {
          print(e.getClass().getName() + ": " + e.getMessage());
        }
      });
      emf.runInTransaction(em -> {
        try {
          em.createNativeQuery("select * from Produce p join Farm f on f.id = p.farm_id", Produce.class)
              .getResultList();
        } catch (RuntimeException e) {
          print(e.getClass().getName() + ": " + e.getMessage());
        }
      });
      emf.runInTransaction(em -> {
        try {
          em.createNamedQuery("Produce.findByCategory", Farm.class);
        } catch (RuntimeException e) {
          print(e.getClass().getName() + ": " + e.getMessage());
        }
      });
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("== " + title + " ==");
  }

  private static void print(Object value) {
    System.out.println("-> " + value);
  }
}
