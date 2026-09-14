package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * A suite of scenarios for one aggregator: every {@code <name>.json} in the given classpath
 * directories becomes its own test case, and its output is compared with {@code
 * <name>.expected.json}.
 *
 * <p>A subclass says three things — where the scenarios are, what the project's resource looks like
 * ({@link #layout()}), and what to run against a loaded scenario ({@link #aggregate}). A new case is
 * then two files and no code at all.
 *
 * <p>A project normally does not extend this class directly but once per kind of aggregator, so that
 * the layout and the aggregation are named in one place:
 *
 * <pre>{@code
 * abstract class SectionTypeScenarioTest<A extends SectionTypeAggregator> extends ScenarioTest {
 *   private final Class<A> type;
 *   private final String componentType;
 *
 *   protected ScenarioLayout layout() {
 *     return MyScenario.LAYOUT;
 *   }
 *
 *   protected String aggregate(ScenarioContext context) {
 *     return context.aggregate(context.aggregator(this.type)::aggregateSectionType, componentType);
 *   }
 * }
 * }</pre>
 *
 * <p>Tests of their own may sit beside the inherited one: a subclass is an ordinary test class, and
 * {@link #load(String)} opens a single scenario for them.
 */
// A scenario suite holds no state between cases, so one instance per class is safe - and it is what
// lets the discovery below be an instance method that can read the subclass's directories.
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ScenarioTest {

  private final List<String> directories;

  /**
   * @param directories the classpath directories holding the scenarios of this suite
   */
  protected ScenarioTest(String... directories) {
    this.directories = List.of(directories);
  }

  /** Where the resource of the project under test keeps its components. */
  protected abstract ScenarioLayout layout();

  /**
   * Runs what this suite is about against a loaded scenario; the result is compared with the golden
   * file.
   *
   * @param context the scenario, loaded and wired
   * @return the aggregated resource as normalized JSON
   */
  protected abstract String aggregate(ScenarioContext context);

  /** Opens a single scenario, for a test beside the discovered ones. */
  protected ScenarioContext load(String resource) {
    return ScenarioContext.load(resource, this.layout());
  }

  /** The scenarios of this suite; the source of the parameterized test below. */
  protected List<Scenario> scenarios() {
    return Scenarios.discover(this.directories.toArray(String[]::new));
  }

  // Protected, not package-private as PMD would have it: the subclasses live in the projects that
  // use this harness, so a package-private case would not be inherited there at all.
  @SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
  @ParameterizedTest(name = "{0}")
  @MethodSource("scenarios")
  protected void producesExpectedOutput(Scenario scenario) {
    ScenarioContext context = this.load(scenario.inputResource());

    assertThat(this.aggregate(context))
        .as("aggregated output for scenario '%s'", scenario.name())
        .isEqualTo(context.expected(scenario.expectedResource()));
  }
}
