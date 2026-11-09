package com.sitepark.ies.aggregator.testkit.probe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sitepark.ies.aggregator.Aggregator;
import com.sitepark.ies.aggregator.Configurable;
import com.sitepark.ies.aggregator.Configuration;
import com.sitepark.ies.aggregator.output.OutputNode;
import com.sitepark.ies.aggregator.resolver.Resolver;
import jakarta.inject.Inject;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** An aggregator that reads its configuration, as production hands it in. */
public final class ConfigurableProbeAggregator
    implements Aggregator, Configurable<ConfigurableProbeAggregator.Config> {

  /** The configuration: the parts of the section type's configuration the aggregator reads. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Config(@Nullable Map<String, String> aggregator) implements Configuration {}

  private @Nullable Config config;

  @Inject
  ConfigurableProbeAggregator() {}

  @Override
  public void configure(Config config) {
    this.config = config;
  }

  /** The configuration as it was handed in, or {@code null} if nobody configured the aggregator. */
  public @Nullable Config config() {
    return this.config;
  }

  @Override
  public void aggregate(Resolver source, OutputNode output) {
    // not used
  }
}
