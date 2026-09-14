package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitepark.ies.aggregator.testkit.probe.OptionlessProbeAggregator;
import com.sitepark.ies.aggregator.testkit.probe.ProbeAggregator;
import org.junit.jupiter.api.Test;

/**
 * A scenario suite as a project writes one — and thereby the test of {@link ScenarioTest} itself:
 * the scenarios of both directories run through the inherited case, which proves that a suite may
 * draw from several.
 *
 * <p>It also shows the two things a suite does beside the inherited test: cases of its own, and
 * {@link #load(String)} for a scenario it names itself.
 */
class ProbeScenarioTest extends ScenarioTest {

  ProbeScenarioTest() {
    super("scenarios/probe", "scenarios/probe-elsewhere");
  }

  @Override
  protected ScenarioLayout layout() {
    return new ScenarioLayout("content", "ROOT", "main", "main");
  }

  @Override
  protected String aggregate(ScenarioContext context) {
    return context.aggregate(context.aggregator(ProbeAggregator.class));
  }

  @Test
  void handsTheAggregatorTheScenariosOptions() {
    ScenarioContext context = this.load("scenarios/probe/configured-greeting.json");

    ProbeAggregator aggregator = context.aggregator(ProbeAggregator.class);

    assertThat(aggregator.options())
        .as("an OptionsAware aggregator is handed the scenario's options, as production does it")
        .isNotNull();
    assertThat(aggregator.options().greeting())
        .as("the values come from the scenario's options block")
        .isEqualTo("Hello");
  }

  @Test
  void handsAnEmptyOptionsInstanceWhenTheScenarioConfiguresNone() {
    ScenarioContext context = this.load("scenarios/probe/plain-headline.json");

    ProbeAggregator aggregator = context.aggregator(ProbeAggregator.class);

    assertThat(aggregator.options())
        .as("without an options block the aggregator still gets an instance, not null")
        .isNotNull();
    assertThat(aggregator.options().greeting()).as("nothing configured means nothing set").isNull();
  }

  @Test
  void buildsAnAggregatorThatHasNoOptions() {
    ScenarioContext context = this.load("scenarios/probe/plain-headline.json");

    assertThat(context.aggregator(OptionlessProbeAggregator.class))
        .as("an aggregator that declares no options is built just as well, and left alone")
        .isNotNull();
  }
}
