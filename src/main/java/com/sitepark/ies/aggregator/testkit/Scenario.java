package com.sitepark.ies.aggregator.testkit;

/**
 * A discovered test scenario: the classpath resources of its input repository data and its golden
 * output.
 *
 * <p>By convention the entry point (the source the aggregation starts from) is the repository entry
 * with
 * the id {@link ScenarioContext#SOURCE_ID}.
 *
 * @param name the scenario name (the input file name without extension), used as the test label
 * @param inputResource the classpath resource of the repository data ({@code <name>.json})
 * @param expectedResource the classpath resource of the golden output ({@code <name>.expected.json})
 */
public record Scenario(String name, String inputResource, String expectedResource) {

  @Override
  public String toString() {
    return this.name;
  }
}
