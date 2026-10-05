package com.howtodoinjava.hibernate.searchboot;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/wines")
public class WineController {

  private final WineSearchService wineSearchService;

  public WineController(WineSearchService wineSearchService) {
    this.wineSearchService = wineSearchService;
  }

  @GetMapping("/search")
  public WinePage search(@RequestParam(defaultValue = "") String q,
                         @RequestParam(required = false) String region,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size,
                         @RequestParam(defaultValue = "relevance") String sort) {
    WineSort wineSort;
    try {
      wineSort = WineSort.valueOf(sort.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown sort: " + sort);
    }
    return wineSearchService.search(q, region, Math.max(page, 0), Math.clamp(size, 1, 50), wineSort);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Wine create(@RequestBody Wine wine) {
    return wineSearchService.save(wine);
  }
}
