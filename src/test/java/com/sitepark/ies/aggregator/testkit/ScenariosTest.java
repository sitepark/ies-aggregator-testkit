package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class ScenariosTest {

  @Test
  void pairsEveryInputWithItsGoldenFile() {
    List<Scenario> scenarios = Scenarios.discover("scenarios/probe");

    assertThat(scenarios)
        .as("every <name>.json becomes a scenario, sorted by name; the golden files are not cases")
        .extracting(Scenario::name)
        .containsExactly("configured-greeting", "plain-headline");
    assertThat(scenarios.get(0).expectedResource())
        .as("a scenario knows where its golden file is")
        .isEqualTo("scenarios/probe/configured-greeting.expected.json");
  }

  @Test
  void keepsTheOrderOfTheDirectoriesItWasGiven() {
    List<Scenario> scenarios = Scenarios.discover("scenarios/probe-elsewhere", "scenarios/probe");

    assertThat(scenarios)
        .as("several directories are read in the given order, each sorted within itself")
        .extracting(Scenario::name)
        .containsExactly("another-headline", "configured-greeting", "plain-headline");
  }

  @Test
  void saysSoWhenADirectoryIsNotOnTheClasspath() {
    assertThatThrownBy(() -> Scenarios.discover("scenarios/probe", "scenarios/nowhere"))
        .as("a mistyped directory is a broken suite, not an empty one")
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("scenarios/nowhere");
  }
}
