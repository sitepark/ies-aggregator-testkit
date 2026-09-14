package com.sitepark.ies.aggregator.testkit.probe;

import com.sitepark.ies.aggregator.Aggregator;
import com.sitepark.ies.aggregator.output.OutputNode;
import com.sitepark.ies.aggregator.resolver.Resolver;
import jakarta.inject.Inject;

/** An aggregator without options — it must be built just as well, and left alone otherwise. */
public final class OptionlessProbeAggregator implements Aggregator {

  @Inject
  OptionlessProbeAggregator() {}

  @Override
  public void aggregate(Resolver source, OutputNode output) {
    output.put("headline", source.value("sp_headline").asString());
  }
}
