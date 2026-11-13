package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitepark.ies.aggregator.testkit.probe.SectionProbeAggregator;
import org.junit.jupiter.api.Test;

/**
 * The harness has to play the section a container hands a section type exactly as {@code
 * AggregatorTag.mergeSection} does: what the section type writes about it joins the section's
 * model flat, the model is created when missing, and outside a container it is dropped - never
 * lifted onto the resource as an area.
 */
class ScenarioSectionTest {

  private static final ScenarioLayout LAYOUT =
      new ScenarioLayout("content", "ROOT", "main", "main");
  private static final ObjectMapper JSON = new ObjectMapper();

  private static JsonNode aggregate(String resource) throws JsonProcessingException {
    ScenarioContext context =
        ScenarioContext.load(resource, LAYOUT, "com.sitepark.ies.aggregator.testkit.probe");
    return JSON.readTree(
        context.aggregateAsSection(context.aggregator(SectionProbeAggregator.class), "probe"));
  }

  @Test
  void writesIntoTheModelOfTheSectionTheContainerHandsIn() throws JsonProcessingException {
    JsonNode resource = aggregate("testkit/section.json");

    assertThat(resource.path("section").path("model").path("headline").asText())
        .as("a section without a model gets one, as <sp:map name=\"_section.model\"/> creates it")
        .isEqualTo("Opens a section");
    assertThat(resource.path("section").path("id").asText())
        .as("the section itself stays the container's")
        .isEqualTo("section-1");
    assertThat(resource.path("section").path("model").has("empty"))
        .as("an entry that renders empty is left out, as the tag's conversion leaves it out")
        .isFalse();
  }

  @Test
  void keepsWhatTheModelAlreadyHolds() throws JsonProcessingException {
    JsonNode model = aggregate("testkit/section-with-model.json").path("section").path("model");

    assertThat(model.path("implicit").asBoolean())
        .as("a key the section type did not write stays")
        .isTrue();
    assertThat(model.path("headline").asText()).isEqualTo("Joins the model");
  }

  @Test
  void dropsTheSectionOutsideAContainer() throws JsonProcessingException {
    JsonNode resource = aggregate("testkit/enclosing-resource.json");

    assertThat(resource.has("section"))
        .as("without a container's section there is nothing to write into, as in production")
        .isFalse();
    assertThat(resource.path("content").isMissingNode())
        .as("a section type that writes only its section produces no content of its own")
        .isTrue();
  }
}
