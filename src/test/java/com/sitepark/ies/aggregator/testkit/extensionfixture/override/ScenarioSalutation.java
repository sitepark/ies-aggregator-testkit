package com.sitepark.ies.aggregator.testkit.extensionfixture.override;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sitepark.ies.aggregator.testkit.ScenarioFixture;
import com.sitepark.ies.aggregator.testkit.extensionfixture.Salutation;
import jakarta.inject.Inject;

/** A scenario-backed {@link Salutation}: what the scenario's {@code salutation} block says. */
public final class ScenarioSalutation implements Salutation {

  public static final String KEY = "salutation";

  private final String text;

  @Inject
  ScenarioSalutation(ScenarioFixture fixture) {
    this.text = fixture.block(KEY, new TypeReference<String>() {}).orElse("");
  }

  @Override
  public String text() {
    return this.text;
  }
}
