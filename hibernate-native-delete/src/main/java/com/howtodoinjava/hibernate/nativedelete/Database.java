package com.howtodoinjava.hibernate.nativedelete;

import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public final class Database {

  /** The date used as "today" in every example, so the results never change. */
  public static final LocalDate TODAY = LocalDate.of(2026, 10, 1);

  static {
    System.setProperty("org.jboss.logging.provider", "slf4j");
  }

  private Database() {
  }

  public static EntityManagerFactory create(boolean showSql) {
    return new HibernatePersistenceConfiguration("native-delete")
        .managedClasses(Coupon.class, Redemption.class, Campaign.class)
        .jdbcUrl("jdbc:h2:mem:store;DB_CLOSE_DELAY=-1")
        .jdbcCredentials("sa", "")
        .schemaToolingAction(Action.CREATE_DROP)
        .showSql(showSql, false, false)
        .createEntityManagerFactory();
  }

  /**
   * Three coupons: welcome (valid), summer and flash (expired on TODAY).
   * welcome is redeemed once. When summerRedeemed is true, summer is redeemed twice.
   */
  public static void seed(EntityManagerFactory emf, boolean summerRedeemed) {
    emf.runInTransaction(em -> {
      Coupon welcome = new Coupon("welcome", 10, LocalDate.of(2026, 12, 31));
      welcome.redeem(LocalDateTime.of(2026, 9, 20, 10, 0));
      Coupon summer = new Coupon("summer", 20, LocalDate.of(2026, 8, 31));
      if (summerRedeemed) {
        summer.redeem(LocalDateTime.of(2026, 8, 5, 12, 30));
        summer.redeem(LocalDateTime.of(2026, 8, 30, 18, 15));
      }
      Coupon flash = new Coupon("flash", 50, LocalDate.of(2026, 9, 15));
      em.persist(welcome);
      em.persist(summer);
      em.persist(flash);
    });
  }

  /** Recreates the foreign key with ON DELETE CASCADE. */
  public static void addOnDeleteCascade(EntityManagerFactory emf) {
    emf.runInTransaction(em -> {
      em.createNativeQuery("alter table redemption drop constraint fk_redemption_coupon")
          .executeUpdate();
      em.createNativeQuery("alter table redemption add constraint fk_redemption_coupon "
          + "foreign key (coupon_id) references coupon (id) on delete cascade")
          .executeUpdate();
    });
  }

  public static long count(EntityManagerFactory emf, String entity) {
    return emf.callInTransaction(em ->
        em.createQuery("select count(*) from " + entity, Long.class).getSingleResult());
  }

  public static long rows(EntityManagerFactory emf, String table) {
    return emf.callInTransaction(em -> ((Number) em
        .createNativeQuery("select count(*) from " + table).getSingleResult()).longValue());
  }
}
