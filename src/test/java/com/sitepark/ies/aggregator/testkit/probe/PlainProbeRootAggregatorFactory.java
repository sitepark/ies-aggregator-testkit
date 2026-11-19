package com.sitepark.ies.aggregator.testkit.probe;

import com.sitepark.ies.aggregator.Aggregator;
import com.sitepark.ies.aggregator.RootAggregator;
import com.sitepark.ies.aggregator.RootAggregatorFactory;
import java.util.List;

/**
 * A root aggregator factory that builds its page without the object type's steps: it takes none of
 * the capabilities, so the harness hands it nothing, and it writes one fixed area of its own.
 */
public class PlainProbeRootAggregatorFactory implements RootAggregatorFactory {

  @Override
  public RootAggregator create(int id) {
    Aggregator own = (source, output) -> output.node("init").put("id", id);
    return new RootAggregator(List.of(own));
  }
}
