package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitepark.ies.aggregator.testkit.probe.PlainProbeRootAggregatorFactory;
import com.sitepark.ies.aggregator.testkit.probe.StepsProbeRootAggregatorFactory;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * A root aggregator factory gets the section types of its object type's steps exactly as the
 * production {@code StepSectionTypes} hands them in: one flat sequence keyed {@code <step>/<section
 * type>}, steps and section types by {@code order} with ties in the scenario's order, a step
 * switched off or not for the channel's nature left out, an editing-only type left out, an
 * SPML-only type without an aggregator.
 */
class ScenarioRootStepsTest {

  private static final ScenarioLayout LAYOUT =
      new ScenarioLayout("content", "ROOT", "main", "main");
  private static final ObjectMapper JSON = new ObjectMapper();

  private static JsonNode steps() throws JsonProcessingException {
    ScenarioContext context =
        ScenarioContext.load(
            "testkit/steps.json", LAYOUT, "com.sitepark.ies.aggregator.testkit.probe");
    return JSON.readTree(context.aggregateRoot(StepsProbeRootAggregatorFactory.class)).get("steps");
  }

  private static List<String> values(JsonNode steps, String field) {
    List<String> values = new ArrayList<>();
    steps.forEach(step -> values.add(step.get(field).asText()));
    return values;
  }

  @Test
  void placesTheSectionTypesOfTheStepsInOrder() throws JsonProcessingException {
    assertThat(values(steps(), "key"))
        .as(
            "first before the two late steps (tie in scenario order), a before b inside late; an"
                + " order given as text counts as its number, a step without section types adds"
                + " none")
        .containsExactly(
            "first/legacy", "first/tie", "late/a", "late/b", "alsoLate/a", "textOrder/z");
  }

  @Test
  void anSpmlOnlySectionTypeComesWithoutAnAggregator() throws JsonProcessingException {
    assertThat(values(steps(), "java"))
        .as("legacy has only its SPML aggregator; the mask without any is not placed at all")
        .containsExactly("false", "true", "true", "true", "true", "true");
  }

  @Test
  void namesASectionTypeByItsIdElseByItsOwnKey() throws JsonProcessingException {
    assertThat(values(steps(), "id"))
        .as("the key's step part is no part of the id")
        .containsExactly("legacy", "tie", "named-a", "b", "a", "z");
  }

  @Test
  void anObjectTypeWithoutStepsHandsInNone() throws JsonProcessingException {
    ScenarioContext context =
        ScenarioContext.load(
            "testkit/steps-without-object-type.json",
            LAYOUT,
            "com.sitepark.ies.aggregator.testkit.probe");

    assertThat(JSON.readTree(context.aggregateRoot(StepsProbeRootAggregatorFactory.class)))
        .as("the factory runs, with nothing placed")
        .isEmpty();
  }

  @Test
  void aFactoryThatTakesNoStepsStillBuildsThePage() throws JsonProcessingException {
    ScenarioContext context =
        ScenarioContext.load(
            "testkit/steps.json", LAYOUT, "com.sitepark.ies.aggregator.testkit.probe");

    assertThat(JSON.readTree(context.aggregateRoot(PlainProbeRootAggregatorFactory.class)))
        .as("its own aggregators run; it is handed nothing")
        .hasSize(1);
  }
}
