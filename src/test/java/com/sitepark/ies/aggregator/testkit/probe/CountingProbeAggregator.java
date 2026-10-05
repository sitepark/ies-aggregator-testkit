package com.sitepark.ies.aggregator.testkit.probe;

import com.sitepark.ies.aggregator.Aggregator;
import com.sitepark.ies.aggregator.output.OutputNode;
import com.sitepark.ies.aggregator.port.ResourceState;
import com.sitepark.ies.aggregator.resolver.Resolver;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counts itself into {@code init.count}, starting from what the enclosing resource holds, and
 * counts its calls in the resource state's store - the two halves of {@link ResourceState}.
 */
public final class CountingProbeAggregator implements Aggregator {

  private final ResourceState resourceState;

  @Inject
  CountingProbeAggregator(ResourceState resourceState) {
    this.resourceState = resourceState;
  }

  @Override
  public void aggregate(Resolver source, OutputNode output) {
    int before =
        this.resourceState
            .enclosing()
            .map(resource -> resource.get("init"))
            .filter(Map.class::isInstance)
            .map(init -> ((Map<?, ?>) init).get("count"))
            .filter(Number.class::isInstance)
            .map(count -> ((Number) count).intValue())
            .orElse(0);
    int calls =
        this.resourceState.attribute(AtomicInteger.class, AtomicInteger::new).incrementAndGet();
    output.put("headline", source.value("sp_headline").asString());
    output.root().node("init").put("count", before + 1);
    output.root().node("init").put("calls", calls);
  }
}
