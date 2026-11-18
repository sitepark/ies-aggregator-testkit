package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitepark.ies.aggregator.testkit.probe.ConfigurableProbeAggregator;
import com.sitepark.ies.aggregator.testkit.probe.OptionlessProbeAggregator;
import org.junit.jupiter.api.Test;

/**
 * A {@code Configurable} aggregator is configured with the scenario's {@code aggregator} block, as
 * the production factory configures it with the section type's - and one without the block gets an
 * empty configuration, not none.
 */
class ScenarioContextConfigurableTest {

  private static final ScenarioLayout LAYOUT =
      new ScenarioLayout("content", "ROOT", "main", "main");

  private static ScenarioContext load(String name) {
    return ScenarioContext.load("testkit/" + name + ".json", LAYOUT);
  }

  @Test
  void handsTheAggregatorTheAggregatorBlockOfTheScenario() {
    ConfigurableProbeAggregator aggregator =
        load("configurable").aggregator(ConfigurableProbeAggregator.class);

    assertThat(aggregator.config()).isNotNull();
    assertThat(aggregator.config().aggregator()).containsEntry("module", "bottrop-module");
  }

  @Test
  void configuresWithAnEmptyConfigurationWhenTheScenarioHasNoBlock() {
    ConfigurableProbeAggregator aggregator =
        load("section-type").aggregator(ConfigurableProbeAggregator.class);

    assertThat(aggregator.config()).isNotNull();
    assertThat(aggregator.config().aggregator()).isNull();
  }

  @Test
  void leavesAnAggregatorThatIsNotConfigurableAlone() {
    assertThat(load("configurable").aggregator(OptionlessProbeAggregator.class)).isNotNull();
  }
}
