package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** The harness keeps the regeneration moment production would keep, and only that one. */
class ScenarioRegenerationTest {

  private static final Instant NOW = Instant.parse("2026-09-01T12:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Test
  void keepsTheEarliestFutureMoment() {
    ScenarioRegeneration regeneration = new ScenarioRegeneration(CLOCK);

    regeneration.at(NOW.plus(Duration.ofDays(3)));
    regeneration.at(NOW.plus(Duration.ofDays(1)));
    regeneration.at(NOW.plus(Duration.ofDays(2)));

    assertThat(regeneration.moment())
        .as("several sections ask; the earliest moment decides")
        .contains(NOW.plus(Duration.ofDays(1)));
  }

  @Test
  void ignoresAMomentThatIsNotInTheFuture() {
    ScenarioRegeneration regeneration = new ScenarioRegeneration(CLOCK);

    regeneration.at(NOW.minus(Duration.ofDays(1)));
    regeneration.at(NOW);

    assertThat(regeneration.moment())
        .as("a moment that has passed cannot be met any more")
        .isEmpty();
  }
}
