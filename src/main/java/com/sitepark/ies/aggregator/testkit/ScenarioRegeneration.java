package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.port.Regeneration;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * {@link Regeneration} of a scenario: keeps the moment the publisher would regenerate the resource
 * at, so a test can ask for it.
 *
 * <p>The same rule as production, where the moment ends up as the date {@code sp:expire} sets: the
 * earliest moment that still lies in the future wins, a past one is ignored. "Now" is the harness's
 * frozen clock, so a scenario's dates decide reproducibly which side of it they fall on.
 */
final class ScenarioRegeneration implements Regeneration {

  private final Clock clock;
  private @Nullable Instant moment;

  ScenarioRegeneration(Clock clock) {
    this.clock = clock;
  }

  @Override
  public void at(Instant moment) {
    if (!moment.isAfter(this.clock.instant())) {
      return;
    }
    if (this.moment == null || moment.isBefore(this.moment)) {
      this.moment = moment;
    }
  }

  Optional<Instant> moment() {
    return Optional.ofNullable(this.moment);
  }
}
