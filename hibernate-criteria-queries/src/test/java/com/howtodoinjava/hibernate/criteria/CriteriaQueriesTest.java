package com.howtodoinjava.hibernate.criteria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Tuple;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.hibernate.Session;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CriteriaQueriesTest {

  private static final String LISTING_COLUMNS =
      "select l1_0.id,l1_0.bedrooms,l1_0.listedOn,l1_0.neighborhood_id,l1_0.price,l1_0.status,l1_0.title from Listing l1_0";

  private final SqlCapture sql = new SqlCapture();
  private EntityManagerFactory emf;

  @BeforeEach
  void setUp() {
    emf = Database.create(false, Map.of("hibernate.session_factory.statement_inspector", sql));
    Database.seed(emf);
    sql.clear();
  }

  @AfterEach
  void tearDown() {
    emf.close();
  }

  private static List<String> names(List<?> rows) {
    return rows.stream().map(Object::toString).toList();
  }

  private List<String> where(ListingQueries.PredicateFactory factory) {
    return names(emf.callInTransaction(em -> ListingQueries.where(em, factory)));
  }

  @Test
  void selectAllListings() {
    assertEquals(List.of("Sunny Loft", "River Cottage", "Family Home", "Corner Condo", "Lake House", "Garden Flat",
        "Studio Nest", "Hilltop Villa"), names(emf.callInTransaction(ListingQueries::findAll)));
    assertEquals(LISTING_COLUMNS, sql.last());
  }

  @Test
  void quickReferenceQuery() {
    assertEquals(List.of("Studio Nest", "Sunny Loft", "Garden Flat"),
        names(emf.callInTransaction(em -> ListingQueries.findActiveUnder(em, new BigDecimal("300000")))));
    assertEquals(LISTING_COLUMNS + " where l1_0.status=? and l1_0.price<? order by l1_0.price", sql.last());
  }

  @Test
  void wherePredicates() {
    assertEquals(List.of("Sunny Loft", "River Cottage", "Lake House", "Garden Flat", "Studio Nest"),
        where(ListingQueries::statusEquals));
    assertTrue(sql.last().endsWith("where l1_0.status=? order by l1_0.id"));

    assertEquals(List.of("Lake House"), where(ListingQueries::titleLike));
    assertTrue(sql.last().endsWith("where l1_0.title like ? escape '' order by l1_0.id"));

    assertEquals(List.of("Sunny Loft", "River Cottage", "Garden Flat"), where(ListingQueries::priceBetween));
    assertTrue(sql.last().endsWith("where l1_0.price between ? and ? order by l1_0.id"));

    assertEquals(List.of("Family Home", "Corner Condo", "Hilltop Villa"), where(ListingQueries::statusIn));
    assertTrue(sql.last().endsWith("where l1_0.status in (?,?) order by l1_0.id"));

    assertEquals(List.of("Studio Nest"), where(ListingQueries::noNeighborhood));
    assertTrue(sql.last().endsWith("where l1_0.neighborhood_id is null order by l1_0.id"));

    assertEquals(List.of("Family Home", "Lake House", "Hilltop Villa"), where(ListingQueries::atLeastThreeBedrooms));
    assertTrue(sql.last().endsWith("where l1_0.bedrooms>=? order by l1_0.id"));

    assertEquals(List.of("Garden Flat", "Studio Nest", "Hilltop Villa"), where(ListingQueries::listedAfter));
    assertTrue(sql.last().endsWith("where l1_0.listedOn>? order by l1_0.id"));

    assertEquals(7, where(ListingQueries::notSold).size());
    assertTrue(sql.last().endsWith("where l1_0.status<>? order by l1_0.id"));
  }

  @Test
  void andOrNot() {
    assertEquals(List.of("River Cottage", "Lake House", "Garden Flat", "Studio Nest"),
        where(ListingQueries::activeAndRoomyOrCheap));
    assertTrue(sql.last().endsWith("where l1_0.status=? and (l1_0.bedrooms>=? or l1_0.price<?) order by l1_0.id"));

    assertEquals(List.of("Family Home", "Corner Condo", "Hilltop Villa"), where(ListingQueries::notActive));
    assertTrue(sql.last().endsWith("where l1_0.status<>? order by l1_0.id"));
  }

  @Test
  void parameterExpressionReusesTheQuery() {
    List<List<Listing>> results = emf.callInTransaction(em ->
        ListingQueries.sameQueryTwoPrices(em, new BigDecimal("200000"), new BigDecimal("300000")));
    assertEquals(List.of("Studio Nest"), names(results.get(0)));
    assertEquals(List.of("Studio Nest", "Corner Condo", "Sunny Loft", "Garden Flat"), names(results.get(1)));
    assertEquals(2, sql.statements().size());
    assertEquals(LISTING_COLUMNS + " where l1_0.price<=? order by l1_0.price", sql.last());
  }

  @Test
  void orderBy() {
    assertEquals(List.of("Hilltop Villa", "Family Home", "Lake House", "Garden Flat", "River Cottage", "Studio Nest",
        "Corner Condo", "Sunny Loft"), names(emf.callInTransaction(ListingQueries::sortedByBedroomsThenPrice)));
    assertTrue(sql.last().endsWith("order by l1_0.bedrooms desc,l1_0.price"));
  }

  @Test
  void projections() {
    assertEquals(List.of("Corner Condo", "Family Home", "Garden Flat", "Hilltop Villa", "Lake House", "River Cottage",
        "Studio Nest", "Sunny Loft"), emf.callInTransaction(ListingQueries::titles));
    assertEquals("select l1_0.title from Listing l1_0 order by 1", sql.last());

    List<Tuple> tuples = emf.callInTransaction(ListingQueries::titleAndPrice);
    assertEquals("Family Home", tuples.get(0).get("title", String.class));
    assertEquals(new BigDecimal("480000.00"), tuples.get(0).get("price", BigDecimal.class));
    assertEquals("Hilltop Villa", tuples.get(1).get(0));
    assertEquals("select l1_0.title,l1_0.price from Listing l1_0 where l1_0.status=? order by 2", sql.last());

    List<Object[]> arrays = emf.callInTransaction(ListingQueries::titleAndPriceArray);
    assertEquals("Family Home", arrays.get(0)[0]);
    assertEquals(new BigDecimal("480000.00"), arrays.get(0)[1]);

    assertEquals(List.of(new ListingSummary("Family Home", new BigDecimal("480000.00")),
            new ListingSummary("Hilltop Villa", new BigDecimal("720000.00"))),
        emf.callInTransaction(ListingQueries::summaries));
    assertEquals("select l1_0.title,l1_0.price from Listing l1_0 where l1_0.status=? order by 2", sql.last());
  }

  @Test
  void innerAndLeftJoins() {
    assertEquals(List.of("Sunny Loft", "River Cottage", "Family Home", "Corner Condo"),
        names(emf.callInTransaction(em -> ListingQueries.inCity(em, "Austin"))));
    assertEquals(LISTING_COLUMNS + " join Neighborhood n1_0 on n1_0.id=l1_0.neighborhood_id where n1_0.city=? "
        + "order by l1_0.id", sql.last());

    List<Tuple> rows = emf.callInTransaction(ListingQueries::titlesWithNeighborhood);
    assertEquals(3, rows.size());
    assertEquals("Studio Nest", rows.get(1).get(0));
    assertNull(rows.get(1).get(1));
    assertEquals("Lakeview", rows.get(2).get(1));
    assertEquals("select l1_0.title,n1_0.name from Listing l1_0 left join Neighborhood n1_0 "
        + "on n1_0.id=l1_0.neighborhood_id where l1_0.listedOn>=? order by l1_0.id", sql.last());
  }

  @Test
  void manyToManyJoinAndDistinct() {
    assertEquals(List.of("Lake House", "Hilltop Villa"),
        names(emf.callInTransaction(em -> ListingQueries.withAmenity(em, "Pool"))));
    assertEquals(LISTING_COLUMNS + " join listing_amenity a1_0 on l1_0.id=a1_0.listing_id join Amenity a1_1 "
        + "on a1_1.id=a1_0.amenity_id where a1_1.name=? order by l1_0.id", sql.last());

    // Entity results: Hibernate removes the duplicate Family Home even without distinct
    assertEquals(List.of("Sunny Loft", "River Cottage", "Family Home", "Lake House", "Garden Flat", "Hilltop Villa"),
        names(emf.callInTransaction(em -> ListingQueries.withAnyAmenity(em, false, "Garage", "Garden"))));
    assertFalse(sql.last().contains("distinct"));
    assertEquals(6, emf.callInTransaction(em -> ListingQueries.withAnyAmenity(em, true, "Garage", "Garden")).size());
    assertTrue(sql.last().startsWith("select distinct l1_0.id"));

    // Scalar results: the duplicate row stays unless we ask for distinct
    assertEquals(List.of("Family Home", "Family Home", "Garden Flat", "Hilltop Villa", "Lake House", "River Cottage",
        "Sunny Loft"), emf.callInTransaction(em -> ListingQueries.titlesWithAnyAmenity(em, false, "Garage", "Garden")));
    assertEquals(List.of("Family Home", "Garden Flat", "Hilltop Villa", "Lake House", "River Cottage", "Sunny Loft"),
        emf.callInTransaction(em -> ListingQueries.titlesWithAnyAmenity(em, true, "Garage", "Garden")));
    assertTrue(sql.last().startsWith("select distinct l1_0.title from Listing l1_0"));
  }

  @Test
  void fetchJoinAvoidsNPlusOne() {
    List<String> lazy = emf.callInTransaction(ListingQueries::labelsWithoutFetch);
    assertEquals(7, lazy.size());
    assertEquals("Sunny Loft (Riverside)", lazy.getFirst());
    assertEquals(4, sql.statements().size());   // 1 for listings + 1 per neighborhood
    assertEquals("select n1_0.id,n1_0.city,n1_0.name from Neighborhood n1_0 where n1_0.id=?", sql.last());

    sql.clear();
    List<String> fetched = emf.callInTransaction(ListingQueries::labelsWithFetch);
    assertEquals(lazy, fetched);
    assertEquals(1, sql.statements().size());
    assertEquals("select l1_0.id,l1_0.bedrooms,l1_0.listedOn,n1_0.id,n1_0.city,n1_0.name,l1_0.price,l1_0.status,"
        + "l1_0.title from Listing l1_0 join Neighborhood n1_0 on n1_0.id=l1_0.neighborhood_id order by l1_0.id",
        sql.last());

    sql.clear();
    List<String> amenities = emf.callInTransaction(em -> ListingQueries.withAmenitiesFetched(em).stream()
        .map(l -> l.getTitle() + " " + l.getAmenities().stream().map(Amenity::getName).sorted().toList())
        .toList());
    assertEquals(List.of("Family Home [Garage, Garden]", "Hilltop Villa [Garden, Pool]"), amenities);
    assertEquals(1, sql.statements().size());
    assertTrue(sql.last().contains("left join listing_amenity a1_0 on l1_0.id=a1_0.listing_id"));
  }

  @Test
  void groupByHaving() {
    assertEquals(List.of(new NeighborhoodStats("Lakeview", 3L, 555000.0), new NeighborhoodStats("Old Town", 2L, 345000.0)),
        emf.callInTransaction(ListingQueries::statsByNeighborhood));
    assertEquals("select n1_0.name c0,count(l1_0.id) c1,avg(l1_0.price) c2 from Listing l1_0 join Neighborhood n1_0 "
        + "on n1_0.id=l1_0.neighborhood_id group by c0 having avg(l1_0.price)>? order by 1", sql.last());

    List<String> byStatus = emf.callInTransaction(ListingQueries::countByStatus).stream()
        .map(row -> row[0] + "=" + row[1]).toList();
    assertEquals(List.of("ACTIVE=5", "PENDING=2", "SOLD=1"), byStatus);
    assertEquals("select l1_0.status c0,count(l1_0.id) c1 from Listing l1_0 group by c0 order by 1", sql.last());
  }

  @Test
  void subqueries() {
    assertEquals(List.of("Family Home", "Lake House", "Hilltop Villa"),
        names(emf.callInTransaction(ListingQueries::pricedAboveAverage)));
    assertEquals(LISTING_COLUMNS + " where l1_0.price>(select avg(l2_0.price) from Listing l2_0) order by l1_0.price",
        sql.last());

    assertEquals(List.of("Hillcrest", "Old Town"), names(emf.callInTransaction(ListingQueries::withoutActiveListings)));
    assertEquals("select n1_0.id,n1_0.city,n1_0.name from Neighborhood n1_0 where not exists(select 1 from Listing l1_0 "
        + "where l1_0.neighborhood_id=n1_0.id and l1_0.status=?) order by n1_0.name", sql.last());
  }

  @Test
  void paginationWithCountQuery() {
    PageResult<Listing> page = emf.callInTransaction(em -> ListingQueries.activePage(em, 2, 2));
    assertEquals(List.of("Garden Flat", "River Cottage"), names(page.content()));
    assertEquals(5, page.totalElements());
    assertEquals(3, page.totalPages());
    assertEquals(LISTING_COLUMNS + " where l1_0.status=? order by l1_0.price,l1_0.id offset ? rows fetch first ? rows only",
        sql.statements().get(0));
    assertEquals("select count(l1_0.id) from Listing l1_0 where l1_0.status=?", sql.statements().get(1));
  }

  @Test
  void dynamicFilters() {
    assertEquals(8, emf.callInTransaction(em -> ListingQueries.search(em, ListingFilter.empty())).size());
    assertEquals(LISTING_COLUMNS + " order by l1_0.price", sql.last());

    assertEquals(List.of("Garden Flat", "Lake House", "Hilltop Villa"), names(emf.callInTransaction(em ->
        ListingQueries.search(em, new ListingFilter("Denver", null, 2, null, null)))));
    assertEquals(LISTING_COLUMNS + " join Neighborhood n1_0 on n1_0.id=l1_0.neighborhood_id where n1_0.city=? "
        + "and l1_0.bedrooms>=? order by l1_0.price", sql.last());

    assertEquals(List.of("Garden Flat"), names(emf.callInTransaction(em -> ListingQueries.search(em,
        new ListingFilter(null, new BigDecimal("300000"), null, ListingStatus.ACTIVE, "garden")))));
    assertEquals(LISTING_COLUMNS + " where l1_0.price<=? and l1_0.status=? and lower(l1_0.title) like ? escape '' "
        + "order by l1_0.price", sql.last());
  }

  @Test
  void valuesAreBoundAsParameters() {
    String attack = "x' or '1'='1";
    assertEquals(0, emf.callInTransaction(em -> ListingQueries.search(em,
        new ListingFilter(null, null, null, null, attack))).size());
    assertFalse(sql.last().contains("1'='1"));
    assertTrue(sql.last().contains("lower(l1_0.title) like ?"));
  }

  @Test
  void criteriaDeleteRemovesJoinTableRowsFirst() {
    int deleted = emf.callInTransaction(em -> ListingQueries.deleteByStatus(em, ListingStatus.SOLD));
    assertEquals(1, deleted);
    assertEquals(List.of(
        "delete from listing_amenity to_delete_ where to_delete_.listing_id in (select l1_0.id from Listing l1_0 where l1_0.status=?)",
        "delete from Listing l1_0 where l1_0.status=?"), sql.statements());
  }

  @Test
  void criteriaUpdate() {
    int updated = emf.callInTransaction(em -> ListingQueries.markOldPendingAsSold(em, LocalDate.of(2026, 9, 10)));
    assertEquals(1, updated);
    assertEquals("update Listing l1_0 set status=? where l1_0.status=? and l1_0.listedOn<?", sql.last());
    assertEquals(List.of("Family Home", "Corner Condo"),
        names(emf.callInTransaction(em -> ListingQueries.where(em, (cb, l) -> cb.equal(l.get("status"), ListingStatus.SOLD)))));

    sql.clear();
    int cut = emf.callInTransaction(em -> ListingQueries.cutPrice(em, new BigDecimal("10000"), 21, LocalDate.of(2026, 10, 3)));
    assertEquals(4, cut);
    assertEquals("update Listing l1_0 set price=(l1_0.price-cast(? as numeric(38,2))) where l1_0.status=? "
        + "and l1_0.listedOn<=?", sql.last());
    List<String> prices = emf.callInTransaction(em -> ListingQueries.findActiveUnder(em, new BigDecimal("1000000")))
        .stream().map(l -> l.getTitle() + "=" + l.getPrice()).toList();
    assertEquals(List.of("Studio Nest=180000.00", "Sunny Loft=240000.00", "Garden Flat=285000.00",
        "River Cottage=310000.00", "Lake House=640000.00"), prices);
  }

  @Test
  void bulkUpdateSkipsLoadedEntities() {
    emf.runInTransaction(em -> {
      Listing home = ListingQueries.where(em, (cb, l) -> cb.equal(l.get("title"), "Family Home")).getFirst();
      ListingQueries.markOldPendingAsSold(em, LocalDate.of(2026, 9, 10));
      assertEquals(ListingStatus.PENDING, home.getStatus());   // stale object in the persistence context
      em.refresh(home);
      assertEquals(ListingStatus.SOLD, home.getStatus());
    });
  }

  @Test
  void criteriaDeleteFailsOnForeignKey() {
    RuntimeException e = assertThrows(RuntimeException.class,
        () -> emf.runInTransaction(em -> ListingQueries.deleteNeighborhood(em, "Riverside")));
    String message = e.getClass().getName() + ": " + e.getMessage();
    System.out.println("FK error: " + message);
    assertTrue(message.contains("Referential integrity constraint violation"), message);
  }

  @Test
  void staticMetamodel() {
    assertEquals(List.of("Corner Condo", "Sunny Loft", "River Cottage"), names(emf.callInTransaction(em ->
        ListingQueries.inCityTypeSafe(em, "Austin", new BigDecimal("400000")))));
    assertEquals(LISTING_COLUMNS + " join Neighborhood n1_0 on n1_0.id=l1_0.neighborhood_id where n1_0.city=? "
        + "and l1_0.price<=? order by l1_0.price", sql.last());
    assertEquals("price", Listing_.PRICE);
    assertEquals(BigDecimal.class, Listing_.price.getJavaType());
  }

  @Test
  void misspelledStringAttributeFailsAtRuntime() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> emf.runInTransaction(ListingQueries::misspelledAttribute));
    assertEquals("org.hibernate.query.sqm.PathElementException", e.getClass().getName());
    assertEquals("Could not resolve attribute 'prise' of 'com.howtodoinjava.hibernate.criteria.Listing'", e.getMessage());
  }

  @Test
  void misspelledMetamodelAttributeFailsAtCompileTime() throws Exception {
    String source = """
        package com.howtodoinjava.hibernate.criteria;
        class Typo {
          Object path(jakarta.persistence.criteria.Root<Listing> listing) {
            return listing.get(Listing_.prise);
          }
        }
        """;
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    StringWriter errors = new StringWriter();
    Path out = Files.createTempDirectory("typo");
    SimpleJavaFileObject file = new SimpleJavaFileObject(URI.create("string:///Typo.java"), SimpleJavaFileObject.Kind.SOURCE) {
      @Override
      public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return source;
      }
    };
    boolean ok = compiler.getTask(errors, null, null,
        List.of("-proc:none", "-d", out.toString(), "-classpath", System.getProperty("java.class.path")),
        null, List.of(file)).call();
    assertFalse(ok);
    assertTrue(errors.toString().contains("cannot find symbol"), errors.toString());
    assertTrue(errors.toString().contains("variable prise"), errors.toString());
    assertTrue(errors.toString().contains("location: class Listing_"), errors.toString());
  }

  @Test
  void numericShortcutsNeedNumbers() throws Exception {
    // cb.gt()/ge()/lt()/le() accept only numbers; dates need greaterThan()/lessThan()
    var method = jakarta.persistence.criteria.CriteriaBuilder.class.getMethod("ge",
        jakarta.persistence.criteria.Expression.class, Number.class);
    assertEquals(Number.class, method.getParameterTypes()[1]);
  }

  @Test
  void hibernateCriteriaBuilderExtras() {
    assertEquals(List.of("Lake House"), names(emf.callInTransaction(em -> ListingQueries.titleContainsIgnoreCase(em, "LAKE"))));
    assertEquals(LISTING_COLUMNS + " where l1_0.title ilike ? escape ''", sql.last());

    assertEquals(5L, emf.callInTransaction(ListingQueries::countFromQuery));
    assertEquals("select count(*) from Listing l1_0 where l1_0.bedrooms>=?", sql.last());

    assertEquals(List.of("Family Home", "Hilltop Villa"), names(emf.callInTransaction(ListingQueries::fromHqlThenRefine)));
    assertEquals(LISTING_COLUMNS + " where l1_0.bedrooms>=2 and l1_0.status=?", sql.last());

    assertInstanceOf(HibernateCriteriaBuilder.class, emf.callInTransaction(em -> em.getCriteriaBuilder()));
    assertInstanceOf(HibernateCriteriaBuilder.class,
        emf.callInTransaction(em -> em.unwrap(Session.class).getCriteriaBuilder()));
  }

  @Test
  void legacyCriteriaApiIsGone() {
    assertThrows(ClassNotFoundException.class, () -> Class.forName("org.hibernate.Criteria"));
    assertThrows(ClassNotFoundException.class, () -> Class.forName("org.hibernate.criterion.Restrictions"));
    assertThrows(ClassNotFoundException.class, () -> Class.forName("org.hibernate.criterion.Example"));
    assertTrue(java.util.Arrays.stream(Session.class.getMethods()).noneMatch(m -> m.getName().equals("createCriteria")));
  }

  @Test
  void hqlAndCriteriaProduceTheSameSql() {
    emf.callInTransaction(em -> ListingQueries.findActiveUnder(em, new BigDecimal("300000")));
    String criteriaSql = sql.last();
    List<Listing> hql = emf.callInTransaction(em -> em.createQuery(
            "from Listing l where l.status = :status and l.price < :max order by l.price", Listing.class)
        .setParameter("status", ListingStatus.ACTIVE)
        .setParameter("max", new BigDecimal("300000"))
        .getResultList());
    assertEquals(List.of("Studio Nest", "Sunny Loft", "Garden Flat"), names(hql));
    assertEquals(criteriaSql, sql.last());
  }

  @Test
  void bulkUpdateNeedsATransaction() {
    try (var em = emf.createEntityManager()) {
      assertThrows(jakarta.persistence.TransactionRequiredException.class,
          () -> ListingQueries.markOldPendingAsSold(em, LocalDate.of(2026, 9, 10)));
    }
  }
}
