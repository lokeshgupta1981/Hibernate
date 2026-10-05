package com.howtodoinjava.hibernate.searchboot;

import java.util.List;

/** One page of search results, returned as JSON by the REST endpoint. */
public record WinePage(String query, long total, int page, int size, List<Wine> wines) {
}
