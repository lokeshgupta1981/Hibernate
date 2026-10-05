package com.howtodoinjava.hibernate.nativescalar;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Tuple;
import java.util.Arrays;
import java.util.List;

public class NativeScalarDemo {

  public static void main(String[] args) {
    try (EntityManagerFactory emf = Database.create(true)) {
      Database.seed(emf);

      emf.runInTransaction(em -> {
        step("1. Named native query without resultClass, one column");
        Object robins = em.createNamedQuery("Sighting.countBySpecies")
            .setParameter("species", "Robin")
            .getSingleResult();
        show(robins);

        step("2. Named native query without resultClass, two columns");
        List<Object[]> totals = em.createNamedQuery("Sighting.totalsBySpecies", Object[].class)
            .getResultList();
        totals.forEach(row -> System.out.println(Arrays.toString(row)
            + " types " + row[0].getClass().getSimpleName() + ", " + row[1].getClass().getSimpleName()));

        step("3. Named native query with a basic resultClass");
        show(em.createNamedQuery("Sighting.lastSeen")
            .setParameter("species", "Robin")
            .getSingleResult());

        step("4. createNativeQuery with a scalar type");
        show(em.createNativeQuery("select count(*) from Sighting", Integer.class).getSingleResult());
        System.out.println(em.createNativeQuery(
                "select distinct species from Sighting order by species", String.class)
            .getResultList());

        step("5. Tuple");
        Tuple tuple = (Tuple) em.createNativeQuery(
                "select species, count, seenOn from Sighting where location = 'Lake Park'", Tuple.class)
            .getSingleResult();
        System.out.println(tuple.get("species") + ", " + tuple.get("count") + ", " + tuple.get("seenOn"));
        tuple.getElements().forEach(e ->
            System.out.println("  " + e.getAlias() + " -> " + e.getJavaType().getSimpleName()));

        step("6. @SqlResultSetMapping with @ColumnResult");
        List<Object[]> byLocation = em.createNamedQuery("Sighting.totalsByLocation", Object[].class)
            .getResultList();
        byLocation.forEach(row -> System.out.println(Arrays.toString(row)
            + " types " + row[0].getClass().getSimpleName() + ", " + row[1].getClass().getSimpleName()));

        step("7. Java types of common expressions");
        Object[] types = (Object[]) em.createNativeQuery(
                "select count(*), sum(count), avg(count), max(count), max(seenOn), min(species) from Sighting")
            .getSingleResult();
        System.out.println(Arrays.toString(types));
        System.out.println(Arrays.stream(types).map(v -> v.getClass().getSimpleName()).toList());

        step("8. Named native UPDATE without a mapping");
        int updated = em.createNamedQuery("Sighting.renameLocation")
            .setParameter("newName", "River Walk")
            .setParameter("oldName", "River Bend")
            .executeUpdate();
        System.out.println("updated = " + updated);
      });
    }
  }

  private static void step(String title) {
    System.out.println();
    System.out.println("== " + title);
  }

  private static void show(Object value) {
    System.out.println(value + " (" + value.getClass().getName() + ")");
  }
}
