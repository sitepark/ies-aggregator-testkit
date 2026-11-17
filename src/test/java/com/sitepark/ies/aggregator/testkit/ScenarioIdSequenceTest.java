package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitepark.ies.aggregator.testkit.probe.ContainerProbeAggregator;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** One counter per name, from 1, across everything drawn on one scenario. */
class ScenarioIdSequenceTest {

  private static final ScenarioLayout LAYOUT =
      new ScenarioLayout("content", "ROOT", "main", "main");

  @Test
  void countsPerNameFromOne() {
    ScenarioIdSequence ids = new ScenarioIdSequence(null);

    assertThat(ids.next("text")).isEqualTo("text-1");
    assertThat(ids.next("text")).isEqualTo("text-2");
    assertThat(ids.next("section")).as("each name has its own counter").isEqualTo("section-1");
  }

  @Test
  void continuesAfterTheIdsTheScenarioSaysWereDrawn() {
    ScenarioIdSequence ids = new ScenarioIdSequence(Map.of("text", 3));

    assertThat(ids.next("text")).isEqualTo("text-4");
    assertThat(ids.next("section")).as("a name not mentioned starts at 1").isEqualTo("section-1");
  }

  @Test
  void refusesACountThatIsNoNumberOrNegative() {
    assertThatThrownBy(() -> new ScenarioIdSequence(Map.of("text", "3")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("text");
    assertThatThrownBy(() -> new ScenarioIdSequence(Map.of("text", -1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ScenarioIdSequence("text"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void runsOnAcrossTheAggregationsOfOneContext() throws JsonProcessingException {
    ScenarioContext context =
        ScenarioContext.load(
            "testkit/container.json", LAYOUT, "com.sitepark.ies.aggregator.testkit.probe");
    ObjectMapper json = new ObjectMapper();
    ContainerProbeAggregator probe = context.aggregator(ContainerProbeAggregator.class);

    String first = json.readTree(context.aggregate(probe)).path("container").path("first").asText();
    String second =
        json.readTree(context.aggregate(probe)).path("container").path("first").asText();

    assertThat(first).as("the scenario starts the counter at 3").isEqualTo("text-4");
    assertThat(second).as("the counter survives the first aggregation").isEqualTo("text-5");
  }
}
