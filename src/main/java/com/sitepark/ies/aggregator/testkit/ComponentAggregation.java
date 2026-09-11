package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.output.Component;
import com.sitepark.ies.aggregator.resolver.Resolver;

/**
 * The one call a component aggregator makes: fill the component, and say whether anything was
 * produced.
 *
 * <p>The harness asks for this instead of for an aggregator type, because that is all it needs — a
 * test passes the aggregator's own method, e.g. {@code aggregator::aggregateSectionType}. What the
 * harness decides afterwards (whether the component reaches the resource at all) depends on the
 * reported result, not on the aggregator's type.
 */
@FunctionalInterface
public interface ComponentAggregation {

  /**
   * Aggregates into the given component.
   *
   * @param source the resolver of the scenario's source
   * @param component the component to aggregate into
   * @return whether content was produced
   */
  boolean aggregate(Resolver source, Component component);
}
