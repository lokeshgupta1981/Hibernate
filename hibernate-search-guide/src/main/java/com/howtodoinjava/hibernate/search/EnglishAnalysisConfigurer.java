package com.howtodoinjava.hibernate.search;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;

/** Defines the "english" analyzer and the "lowercase" normalizer used in Episode. */
public class EnglishAnalysisConfigurer implements LuceneAnalysisConfigurer {

  @Override
  public void configure(LuceneAnalysisConfigurationContext context) {
    context.analyzer("english").custom()
        .tokenizer("standard")
        .tokenFilter("lowercase")
        .tokenFilter("snowballPorter").param("language", "English")
        .tokenFilter("asciiFolding");

    context.normalizer("lowercase").custom()
        .tokenFilter("lowercase")
        .tokenFilter("asciiFolding");
  }
}
