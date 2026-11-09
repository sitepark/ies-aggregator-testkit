package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.OutOfScopeException;
import com.google.inject.ProvisionException;
import com.sitepark.ies.aggregator.testkit.probe.ChannelInConstructorProbeAggregator;
import com.sitepark.ies.aggregator.testkit.probe.ChannelProviderProbeAggregator;
import org.junit.jupiter.api.Test;

/**
 * An aggregator is built outside the aggregation scope, as {@code AggregatorTag.doStartTag} builds
 * it, so one that injects the scope-bound {@code Channel} fails here as it fails in production.
 */
class ScenarioContextScopeTest {

  private static final ScenarioLayout LAYOUT =
      new ScenarioLayout("content", "ROOT", "main", "main");

  private static ScenarioContext load() {
    return ScenarioContext.load("testkit/section-type.json", LAYOUT);
  }

  @Test
  void refusesAChannelInTheConstructorOfAnAggregator() {
    ScenarioContext context = load();

    assertThatThrownBy(() -> context.aggregator(ChannelInConstructorProbeAggregator.class))
        .isInstanceOf(ProvisionException.class)
        .hasRootCauseInstanceOf(OutOfScopeException.class)
        .rootCause()
        .hasMessageContaining("channelId");
  }

  @Test
  void buildsAnAggregatorThatAsksForTheChannelWhileItAggregates() {
    ChannelProviderProbeAggregator aggregator =
        load().aggregator(ChannelProviderProbeAggregator.class);

    assertThat(aggregator).isNotNull();
  }

  @Test
  void aStrictBuildDoesNotLeakIntoTheNext() {
    ScenarioContext context = load();
    try {
      context.aggregator(ChannelInConstructorProbeAggregator.class);
    } catch (ProvisionException expected) {
      // the build above is refused; what follows must not be
    }

    assertThat(context.aggregator(ChannelProviderProbeAggregator.class)).isNotNull();
  }
}
