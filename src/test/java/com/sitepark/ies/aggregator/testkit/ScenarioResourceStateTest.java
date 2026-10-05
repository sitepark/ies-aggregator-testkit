package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitepark.ies.aggregator.testkit.probe.CountingProbeAggregator;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

/**
 * The harness has to play the resource the template builds around a section exactly as the tag
 * does - what earlier sections left is visible, a section's writes are merged into it flat per
 * area - and must show no enclosing resource on the paths without a tag.
 */
class ScenarioResourceStateTest {

  private static final ScenarioLayout LAYOUT =
      new ScenarioLayout("content", "ROOT", "main", "main");
  private static final ObjectMapper JSON = new ObjectMapper();

  private static ScenarioContext load(String resource) {
    return ScenarioContext.load(resource, LAYOUT, "com.sitepark.ies.aggregator.testkit.probe");
  }

  private static JsonNode init(String json) throws JsonProcessingException {
    return JSON.readTree(json).path("init");
  }

  @Test
  void sectionReadsTheEnclosingResourceAndIsMergedIntoItFlat() throws JsonProcessingException {
    ScenarioContext context = load("testkit/enclosing-resource.json");

    String json =
        context.aggregateAsSection(context.aggregator(CountingProbeAggregator.class), "probe");

    assertThat(init(json).path("count").asInt())
        .as("the section starts from the count an earlier section left")
        .isEqualTo(2);
    assertThat(init(json).path("kept").asText())
        .as("a key the section did not write stays, as AggregatorTag.mergeAreas leaves it")
        .isEqualTo("from an earlier section");
    assertThat(JSON.readTree(json).path("base").path("title").asText())
        .as("an area the section did not touch stays as well")
        .isEqualTo("Start page");
  }

  @Test
  void sectionsOnOneContextSeeEachOtherAsOnOnePage() throws JsonProcessingException {
    ScenarioContext context = load("testkit/enclosing-resource.json");
    CountingProbeAggregator aggregator = context.aggregator(CountingProbeAggregator.class);

    context.aggregateAsSection(aggregator, "probe");
    String second = context.aggregateAsSection(aggregator, "probe");

    assertThat(init(second).path("count").asInt()).isEqualTo(3);
    assertThat(init(second).path("calls").asInt())
        .as("the store lives as long as the resource, across sections")
        .isEqualTo(2);
  }

  @Test
  void noEnclosingResourceWithoutTheTag() throws JsonProcessingException {
    ScenarioContext context = load("testkit/enclosing-resource.json");

    String json = context.aggregate(context.aggregator(CountingProbeAggregator.class));

    assertThat(init(json).path("count").asInt())
        .as("an aggregator building the resource itself has nothing enclosing it")
        .isEqualTo(1);
  }

  @Test
  void enclosingResourceIsReadOnly() {
    ScenarioResourceState state = new ScenarioResourceState();
    state.enterTag(new HashMap<>());

    assertThat(state.enclosing()).isPresent();
    assertThatThrownBy(() -> state.enclosing().orElseThrow().put("init", "x"))
        .isInstanceOf(UnsupportedOperationException.class);

    state.exitTag();
    assertThat(state.enclosing()).isEmpty();
  }
}
