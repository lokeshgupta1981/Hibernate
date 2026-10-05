package com.howtodoinjava.hibernate.searchboot;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.springframework.stereotype.Component;

/**
 * A Spring bean that defines the analyzers and normalizers used in the Wine mapping.
 * application.properties references it with "bean:wineAnalysisConfigurer".
 */
@Component("wineAnalysisConfigurer")
public class WineAnalysisConfigurer implements LuceneAnalysisConfigurer {

  @Override
  public void configure(LuceneAnalysisConfigurationContext context) {
    context.analyzer("wine").custom()
        .tokenizer("standard")
        .tokenFilter("lowercase")
        .tokenFilter("snowballPorter").param("language", "English")
        .tokenFilter("asciiFolding");

    context.normalizer("lowercase").custom()
        .tokenFilter("lowercase")
        .tokenFilter("asciiFolding");
  }
}
