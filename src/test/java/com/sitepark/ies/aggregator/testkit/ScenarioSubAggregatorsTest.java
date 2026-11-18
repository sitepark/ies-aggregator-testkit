package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitepark.ies.aggregator.SubSectionType;
import com.sitepark.ies.aggregator.testkit.probe.ConfigurableProbeAggregator;
import com.sitepark.ies.aggregator.testkit.probe.PlacingProbeAggregator;
import com.sitepark.ies.aggregator.testkit.probe.SectionTypeProbeAggregator;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * A container has to get its children and their configuration exactly as the production {@code
 * AggregatorFactory} hands them in, and the harness has to play the node it fills as {@code
 * AggregatorTag.mergeContainer} does - not more generously.
 */
class ScenarioSubAggregatorsTest {

  private static final ScenarioLayout LAYOUT =
      new ScenarioLayout("content", "ROOT", "main", "main");
  private static final ObjectMapper JSON = new ObjectMapper();

  private static ScenarioContext context(String resource) {
    return ScenarioContext.load(resource, LAYOUT, "com.sitepark.ies.aggregator.testkit.probe");
  }

  @Test
  void createsAChildOnlyWhereItsJavaClassApplies() {
    PlacingProbeAggregator container =
        context("testkit/placing.json").aggregator(PlacingProbeAggregator.class);

    assertThat(container.subAggregators())
        .as(
            "no class, or a class inherited from another template, gets no child; fixed by"
                + " position")
        .containsOnlyKeys("named", "configured", "#fixed:1");
  }

  @Test
  void configuresAChildFromItsOwnSectionType() {
    PlacingProbeAggregator container =
        context("testkit/placing.json").aggregator(PlacingProbeAggregator.class);

    assertThat(
            ((ConfigurableProbeAggregator) container.subAggregators().get("configured")).config())
        .as("the aggregator node without the class name, as ConfigMapper hands it in")
        .isEqualTo(
            new ConfigurableProbeAggregator.Config(
                Map.of(
                    "uri", "/aggregator/sectionTypes/configured.spml",
                    "classNameFor", "/aggregator/sectionTypes/configured.spml",
                    "colour", "red")));
  }

  @Test
  void tellsAChildTheIdOfItsSectionType() {
    PlacingProbeAggregator container =
        context("testkit/placing.json").aggregator(PlacingProbeAggregator.class);

    assertThat(
            ((SectionTypeProbeAggregator) container.subAggregators().get("#fixed:1")).sectionType())
        .as("a fixed section type is named by its id, not by its position")
        .isEqualTo("fixedNamed");
  }

  @Test
  void handsInTheConfigurationOfEveryOfferedSectionType() {
    PlacingProbeAggregator container =
        context("testkit/placing.json").aggregator(PlacingProbeAggregator.class);

    assertThat(container.subSectionTypes())
        .as("also of those without Java, keyed as the children are")
        .isEqualTo(
            Map.of(
                "named",
                new SubSectionType("named", true, null, "probeType", null, null),
                "configured",
                new SubSectionType("configured", true, null, null, null, null),
                "inherited",
                new SubSectionType("inherited", true, null, null, null, null),
                "legacy",
                new SubSectionType("legacy", false, null, null, null, null),
                "#fixed:1",
                new SubSectionType("fixedNamed", true, null, null, null, null)));
  }

  @Test
  void appendsTheEntriesToTheNodeTheTemplateFills() throws JsonProcessingException {
    ScenarioContext context = context("testkit/placing.json");

    JsonNode resource =
        JSON.readTree(
            context.aggregateAsSection(
                context.aggregator(PlacingProbeAggregator.class), "iterableSectionTypeSelector"));

    assertThat(resource.path("container").path("items").toString())
        .as("behind the items already there, in their order, each as its child wrote it")
        .isEqualTo(
            "[{\"id\":\"earlier-1\",\"type\":\"earlier\"},"
                + "{\"id\":\"named\",\"sectionType\":\"named\",\"type\":\"probeType\"},"
                + "{\"id\":\"configured\",\"type\":\"configured\"},"
                + "{\"id\":\"#fixed:1\",\"sectionType\":\"fixedNamed\",\"type\":\"fixedNamed\"}]");
  }

  @Test
  void theContainerItselfDoesNotReachThePage() throws JsonProcessingException {
    ScenarioContext context = context("testkit/placing.json");

    JsonNode resource =
        JSON.readTree(
            context.aggregateAsSection(
                context.aggregator(PlacingProbeAggregator.class), "iterableSectionTypeSelector"));

    assertThat(resource.path("content").isMissingNode())
        .as("a container writes nothing into its own node, so the tag does not add it")
        .isTrue();
  }

  @Test
  void dropsTheEntriesWithoutANodeToFill() throws JsonProcessingException {
    ScenarioContext context = context("testkit/placing-without-container.json");

    JsonNode resource =
        JSON.readTree(
            context.aggregateAsSection(
                context.aggregator(PlacingProbeAggregator.class), "iterableSectionTypeSelector"));

    assertThat(resource.has("container"))
        .as(
            "the node is no area: without a caller's node the entries are dropped, as in"
                + " production")
        .isFalse();
  }
}
