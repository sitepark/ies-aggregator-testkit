package com.sitepark.ies.aggregator.testkit;

import com.google.inject.OutOfScopeException;
import com.sitepark.ies.aggregator.port.Channel;
import jakarta.inject.Provider;
import java.util.function.Supplier;

/**
 * Stands for the production {@code AggregationScope} in the one respect a scenario can get wrong:
 * what may be injected while an aggregator is being built.
 *
 * <p>{@code AggregatorTag.doStartTag} creates the section aggregator <em>before</em> it enters the
 * scope that seeds the channel id; {@code FileGenerator} creates the root aggregator, and with it the
 * same section aggregators, inside the scope. An aggregator has to work on both paths, so the harness
 * builds it the stricter way: outside the scope. A {@link Channel} - production derives it from the
 * scope-bound {@code channelId} - cannot be had there, and an aggregator that injects it fails with
 * an {@code OutOfScopeException}; it takes a {@code Provider<Channel>} and asks while it aggregates.
 *
 * <p>The assemblers an aggregator asks the assembler factory for while it aggregates are created
 * inside the scope, in production and here, so they may inject a {@link Channel}.
 */
final class ScenarioAggregationScope {

  private static final ThreadLocal<Boolean> BUILDING_AGGREGATOR = new ThreadLocal<>();

  private ScenarioAggregationScope() {}

  /** Runs {@code construction} with no aggregation scope open, as the aggregator tag builds. */
  static <T> T outside(Supplier<T> construction) {
    Boolean before = BUILDING_AGGREGATOR.get();
    BUILDING_AGGREGATOR.set(Boolean.TRUE);
    try {
      return construction.get();
    } finally {
      if (before == null) {
        BUILDING_AGGREGATOR.remove();
      } else {
        BUILDING_AGGREGATOR.set(before);
      }
    }
  }

  /** A {@link Channel} binding that refuses to answer while an aggregator is being built. */
  static Provider<Channel> channel(Channel current) {
    return () -> {
      if (Boolean.TRUE.equals(BUILDING_AGGREGATOR.get())) {
        throw new OutOfScopeException(
            "Cannot access Key[Integer @Named(channelId)] outside of a scoping block: the Channel"
                + " is bound to the aggregation scope, which is not open while an aggregator is"
                + " built. Inject a Provider<Channel> and ask in aggregate().");
      }
      return current;
    };
  }
}
