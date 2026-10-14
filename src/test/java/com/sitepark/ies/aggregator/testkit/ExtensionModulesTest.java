package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sitepark.ies.aggregator.testkit.extensionfixture.SalutationAggregator;
import com.sitepark.ies.aggregator.testkit.extensionfixture.base.SalutationExtension;
import org.junit.jupiter.api.Test;

/**
 * The harness installs the modules the extensions of the scanned packages contribute, as the
 * production {@code ExtensionFactory} does - so an assembler finds the binding its JAR ships, and a
 * project's test extension can replace it with a scenario-backed one.
 */
class ExtensionModulesTest {

  private static final ScenarioLayout LAYOUT =
      new ScenarioLayout("content", "ROOT", "main", "main");

  private static final String FIXTURE = "com.sitepark.ies.aggregator.testkit.extensionfixture";

  private static final String SCENARIO = "testkit/salutation.json";

  @Test
  void anAssemblerGetsTheBindingItsJarShips() {
    ScenarioContext context = ScenarioContext.load(SCENARIO, LAYOUT, FIXTURE + ".base");

    assertThat(salutation(context)).isEqualTo(SalutationExtension.DEFAULT);
  }

  @Test
  void aTestExtensionReplacesItWithTheScenarioBlock() {
    ScenarioContext context =
        ScenarioContext.load(SCENARIO, LAYOUT, FIXTURE + ".base", FIXTURE + ".override");

    assertThat(salutation(context)).isEqualTo("from the scenario");
  }

  @Test
  void aScenarioWithoutTheBlockHasNone() {
    ScenarioContext context =
        ScenarioContext.load(
            "testkit/enclosing-resource.json", LAYOUT, FIXTURE + ".base", FIXTURE + ".override");

    assertThat(salutation(context)).isEmpty();
  }

  @Test
  void aConfigureMethodThatIsNotStaticIsRefused() {
    assertThatThrownBy(() -> ScenarioContext.load(SCENARIO, LAYOUT, "invalidextension.nonstatic"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("must be static");
  }

  @Test
  void aFailingConfigureMethodNamesTheExtension() {
    assertThatThrownBy(() -> ScenarioContext.load(SCENARIO, LAYOUT, "invalidextension.failing"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("FailingExtension");
  }

  private static String salutation(ScenarioContext context) {
    return context.aggregator(SalutationAggregator.class).salutation();
  }
}
