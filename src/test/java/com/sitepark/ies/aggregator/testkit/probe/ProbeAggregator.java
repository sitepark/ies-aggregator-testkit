package com.sitepark.ies.aggregator.testkit.probe;

import com.sitepark.ies.aggregator.Aggregator;
import com.sitepark.ies.aggregator.OptionsAware;
import com.sitepark.ies.aggregator.output.OutputNode;
import com.sitepark.ies.aggregator.resolver.Resolver;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

/**
 * An aggregator that writes a headline and, if the scenario configures one, a greeting.
 *
 * <p>Stands in for the aggregators of a project using the harness: package-private constructor with
 * {@code @Inject} like a real one, so that only the injector can build it, and {@link OptionsAware}
 * so that the options have to arrive the way production delivers them.
 */
public final class ProbeAggregator implements Aggregator, OptionsAware<ProbeOptions> {

  private @Nullable ProbeOptions options;

  @Inject
  ProbeAggregator() {}

  @Override
  public void setOptions(ProbeOptions options) {
    this.options = options;
  }

  /** The options as they were handed in, or {@code null} if nobody handed any in. */
  public @Nullable ProbeOptions options() {
    return this.options;
  }

  @Override
  public void aggregate(Resolver source, OutputNode output) {
    output.put("headline", source.value("sp_headline").asString());
    if (this.options != null && this.options.greeting() != null) {
      output.put("greeting", this.options.greeting());
    }
  }
}
