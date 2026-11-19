package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The harness records what the publisher would store, and drops what the production adapter drops:
 * a scenario must not show a dependency that never reaches the publisher.
 */
class ScenarioModuleDependenciesTest {

  @Test
  void recordsAnInstalledModuleWithItsVersionOnce() {
    ScenarioModuleDependencies dependencies =
        new ScenarioModuleDependencies(Map.of("customer", "3.4.0"));

    dependencies.dependsOn("customer");
    dependencies.dependsOn("customer");

    assertThat(dependencies.reported()).containsExactly(new ModuleDependency("customer", "3.4.0"));
  }

  @Test
  void dropsAModuleTheEnvironmentDoesNotKnow() {
    ScenarioModuleDependencies dependencies =
        new ScenarioModuleDependencies(Map.of("customer", "3.4.0"));

    dependencies.dependsOn("customer-module");

    assertThat(dependencies.reported()).isEmpty();
  }

  @Test
  void answersTheInstalledVersionAndReportsTheDependency() {
    ScenarioModuleDependencies dependencies =
        new ScenarioModuleDependencies(Map.of("customer", "3.4.0", "other", "1.0.0"));

    assertThat(dependencies.version("customer")).contains("3.4.0");

    assertThat(dependencies.reported()).containsExactly(new ModuleDependency("customer", "3.4.0"));
  }

  @Test
  void answersNothingAndReportsNothingForAModuleTheScenarioDoesNotName() {
    ScenarioModuleDependencies dependencies =
        new ScenarioModuleDependencies(Map.of("customer", "3.4.0"));

    assertThat(dependencies.version("customer-module")).isEmpty();

    assertThat(dependencies.reported()).isEmpty();
  }

  @Test
  void aScenarioWithoutModulesHasNoneInstalled() {
    ScenarioModuleDependencies dependencies = new ScenarioModuleDependencies(Map.of());

    dependencies.dependsOn("customer");

    assertThat(dependencies.reported()).isEmpty();
  }
}
