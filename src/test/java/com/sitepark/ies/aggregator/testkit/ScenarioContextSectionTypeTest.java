package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sitepark.ies.aggregator.testkit.probe.OptionlessProbeAggregator;
import com.sitepark.ies.aggregator.testkit.probe.SectionTypeProbeAggregator;
import org.junit.jupiter.api.Test;

/**
 * A {@code SectionTypeAware} aggregator is handed the section type id the scenario names, as the
 * production factory hands it the one from the configuration - and the harness never makes one up.
 */
class ScenarioContextSectionTypeTest {

  private static final ScenarioLayout LAYOUT =
      new ScenarioLayout("content", "ROOT", "main", "main");

  private static ScenarioContext load(String name) {
    return ScenarioContext.load("testkit/" + name + ".json", LAYOUT);
  }

  @Test
  void handsTheAggregatorTheSectionTypeOfTheScenario() {
    SectionTypeProbeAggregator aggregator =
        load("section-type").aggregator(SectionTypeProbeAggregator.class);

    assertThat(aggregator.sectionType()).isEqualTo("notification");
  }

  @Test
  void failsWhenTheScenarioNamesNoSectionType() {
    ScenarioContext context = load("no-section-type");

    assertThatThrownBy(() -> context.aggregator(SectionTypeProbeAggregator.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("sectionType");
  }

  @Test
  void failsWhenTheScenarioSectionTypeIsBlank() {
    ScenarioContext context = load("blank-section-type");

    assertThatThrownBy(() -> context.aggregator(SectionTypeProbeAggregator.class))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void doesNotRequireASectionTypeFromAnAggregatorThatIsNotSectionTypeAware() {
    ScenarioContext context = load("no-section-type");

    assertThat(context.aggregator(OptionlessProbeAggregator.class)).isNotNull();
  }
}
