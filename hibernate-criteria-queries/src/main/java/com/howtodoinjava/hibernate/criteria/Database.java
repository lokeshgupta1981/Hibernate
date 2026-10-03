package com.howtodoinjava.hibernate.criteria;

import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  public static EntityManagerFactory create(boolean showSql) {
    return create(showSql, Map.of());
  }

  public static EntityManagerFactory create(boolean showSql, Map<String, Object> settings) {
    HibernatePersistenceConfiguration config = new HibernatePersistenceConfiguration("criteria")
        .managedClasses(Neighborhood.class, Amenity.class, Listing.class)
        .jdbcUrl("jdbc:h2:mem:listings;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false);
    settings.forEach(config::property);
    return config.createEntityManagerFactory();
  }

  /** Saves 4 neighborhoods, 3 amenities and 8 listings. Hillcrest has no listings, Studio Nest has no neighborhood. */
  public static void seed(EntityManagerFactory emf) {
    emf.runInTransaction(em -> {
      Neighborhood riverside = new Neighborhood("Riverside", "Austin");
      Neighborhood oldTown = new Neighborhood("Old Town", "Austin");
      Neighborhood lakeview = new Neighborhood("Lakeview", "Denver");
      Neighborhood hillcrest = new Neighborhood("Hillcrest", "Denver");
      em.persist(riverside);
      em.persist(oldTown);
      em.persist(lakeview);
      em.persist(hillcrest);

      Amenity pool = new Amenity("Pool");
      Amenity garage = new Amenity("Garage");
      Amenity garden = new Amenity("Garden");
      em.persist(pool);
      em.persist(garage);
      em.persist(garden);

      em.persist(listing("Sunny Loft", "250000", 1, 1, ListingStatus.ACTIVE, riverside, garage));
      em.persist(listing("River Cottage", "320000", 2, 3, ListingStatus.ACTIVE, riverside, garden));
      em.persist(listing("Family Home", "480000", 4, 5, ListingStatus.PENDING, oldTown, garage, garden));
      em.persist(listing("Corner Condo", "210000", 1, 8, ListingStatus.SOLD, oldTown));
      em.persist(listing("Lake House", "650000", 3, 10, ListingStatus.ACTIVE, lakeview, pool, garage));
      em.persist(listing("Garden Flat", "295000", 2, 12, ListingStatus.ACTIVE, lakeview, garden));
      em.persist(listing("Studio Nest", "180000", 1, 15, ListingStatus.ACTIVE, null));
      em.persist(listing("Hilltop Villa", "720000", 5, 18, ListingStatus.PENDING, lakeview, pool, garden));
    });
  }

  private static Listing listing(String title, String price, int bedrooms, int day, ListingStatus status,
      Neighborhood neighborhood, Amenity... amenities) {
    Listing listing = new Listing(title, new BigDecimal(price), bedrooms, LocalDate.of(2026, 9, day), status,
        neighborhood);
    listing.getAmenities().addAll(java.util.List.of(amenities));
    return listing;
  }
}
