package com.howtodoinjava.hibernate.searchboot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.search.backend.directory.root=target/test-index")
@AutoConfigureMockMvc
class WineSearchTest {

  @Autowired
  MockMvc mvc;

  @Autowired
  WineSearchService wineSearchService;

  @Autowired
  WineRepository wineRepository;

  @Autowired
  TransactionTemplate transactionTemplate;

  @Autowired
  ApplicationContext context;

  private List<String> names(WinePage page) {
    return page.wines().stream().map(Wine::getName).toList();
  }

  private WinePage search(String text) {
    return wineSearchService.search(text, null, 0, 10, WineSort.RELEVANCE);
  }

  @Test
  void massIndexerIndexedTheRowsFromDataSql() {
    assertThat(wineRepository.count()).isEqualTo(8);
    assertThat(search("").total()).isEqualTo(8);
    assertThat(Files.isDirectory(Path.of("target/test-index/Wine"))).isTrue();
  }

  @Test
  void analysisConfigurerIsASpringBean() {
    assertThat(context.getBean("wineAnalysisConfigurer")).isInstanceOf(WineAnalysisConfigurer.class);
  }

  @Test
  void cherryMatchesBestFirst() {
    assertThat(names(search("cherry"))).containsExactly("Chianti Classico", "Barolo", "Rioja Reserva");
  }

  @Test
  void stemmingFindsCherriesForCherry() {
    assertThat(names(search("cherries"))).containsExactly("Chianti Classico", "Barolo", "Rioja Reserva");
    assertThat(names(search("CHERRY"))).containsExactly("Chianti Classico", "Barolo", "Rioja Reserva");
  }

  @Test
  void typoFindsNothing() {
    assertThat(search("chery").total()).isZero();
  }

  @Test
  void regionFilterIsCaseInsensitive() {
    assertThat(names(search("plum"))).containsExactlyInAnyOrder("Rioja Reserva", "Malbec Reserva");
    WinePage rioja = wineSearchService.search("plum", "rioja", 0, 10, WineSort.RELEVANCE);
    assertThat(names(rioja)).containsExactly("Rioja Reserva");
  }

  @Test
  void sortByPrice() {
    WinePage cheapFirst = wineSearchService.search("cherry", null, 0, 10, WineSort.PRICE_ASC);
    assertThat(names(cheapFirst)).containsExactly("Chianti Classico", "Rioja Reserva", "Barolo");
    WinePage expensiveFirst = wineSearchService.search("cherry", null, 0, 10, WineSort.PRICE_DESC);
    assertThat(names(expensiveFirst)).containsExactly("Barolo", "Rioja Reserva", "Chianti Classico");
  }

  @Test
  void secondPageSortedByName() {
    WinePage page = wineSearchService.search("", null, 1, 3, WineSort.NAME);
    assertThat(page.total()).isEqualTo(8);
    assertThat(names(page)).containsExactly("Malbec Reserva", "Napa Cabernet", "Prosecco");
  }

  @Test
  void restEndpointReturnsOnePage() throws Exception {
    mvc.perform(get("/wines/search").param("q", "cherry").param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.query").value("cherry"))
        .andExpect(jsonPath("$.total").value(3))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.wines", hasSize(2)))
        .andExpect(jsonPath("$.wines[*].name", contains("Chianti Classico", "Barolo")))
        .andExpect(jsonPath("$.wines[0].price").value(19.50));
  }

  @Test
  void restEndpointSortsAndFilters() throws Exception {
    mvc.perform(get("/wines/search").param("q", "cherry").param("sort", "price_asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.wines[*].name", contains("Chianti Classico", "Rioja Reserva", "Barolo")));
    mvc.perform(get("/wines/search").param("q", "plum").param("region", "rioja"))
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.wines[0].name").value("Rioja Reserva"));
  }

  @Test
  void unknownSortIsBadRequest() throws Exception {
    mvc.perform(get("/wines/search").param("q", "cherry").param("sort", "year"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void savedWineIsSearchableAfterCommitAndGoneAfterDelete() throws Exception {
    mvc.perform(post("/wines").contentType(MediaType.APPLICATION_JSON).content("""
            {"name":"Beaujolais","region":"Burgundy","grape":"Gamay",
             "tastingNotes":"Fresh red cherries and strawberry.","price":15.00}"""))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber());

    mvc.perform(get("/wines/search").param("q", "cherry").param("sort", "price_asc").param("size", "2"))
        .andExpect(jsonPath("$.total").value(4))
        .andExpect(jsonPath("$.wines[*].name", contains("Beaujolais", "Chianti Classico")));
    mvc.perform(get("/wines/search").param("region", "BURGUNDY").param("sort", "name"))
        .andExpect(jsonPath("$.wines[*].name", contains("Beaujolais", "Chablis")));

    Wine beaujolais = search("gamay").wines().get(0);
    wineRepository.delete(beaujolais);
    assertThat(search("gamay").total()).isZero();
    assertThat(search("cherry").total()).isEqualTo(3);
  }

  @Test
  void uncommittedWineIsNotSearchableInTheSameTransaction() {
    long insideTransaction = transactionTemplate.execute(status -> {
      wineRepository.saveAndFlush(new Wine("Pinot Noir", "Oregon", "Pinot Noir",
          "Raspberry and earthy notes.", new BigDecimal("32.00")));
      return search("raspberry").total();
    });
    assertThat(insideTransaction).isZero();

    WinePage afterCommit = search("raspberry");
    assertThat(names(afterCommit)).containsExactly("Pinot Noir");

    wineRepository.delete(afterCommit.wines().get(0));
    assertThat(search("raspberry").total()).isZero();
  }
}
