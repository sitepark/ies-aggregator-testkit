package com.sitepark.ies.aggregator.testkit.probe;

import com.sitepark.ies.aggregator.Aggregator;
import com.sitepark.ies.aggregator.output.OutputNode;
import com.sitepark.ies.aggregator.resolver.Resolver;

/**
 * Describes its enclosing section the way {@code newSection} does - into the node {@code section}
 * beside its output - and produces no content of its own.
 */
public final class SectionProbeAggregator implements Aggregator {

  @Override
  public void aggregate(Resolver source, OutputNode output) {
    OutputNode section = output.root().node("section");
    section.put("modelType", "content.section");
    section.put("headline", source.value("sp_headline").asString());
    section.put("empty", "");
  }
}
