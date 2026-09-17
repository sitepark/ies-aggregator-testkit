package com.sitepark.ies.aggregator.testkit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * What a scenario says about the channel it aggregates for: its nature and its configured
 * attributes.
 *
 * <p>Both are absent by default, which is what a production channel answers when nothing is
 * configured for it. A scenario that needs an intranet, or a product switch, has to say so — the
 * harness never invents one, because a rule that reads a channel property has to be shown to read
 * it.
 *
 * @param nature the channel's nature, or {@code null} if the scenario names none
 * @param attributes the configured attributes, empty if the scenario names none
 */
record ScenarioChannelConfig(@Nullable String nature, Map<String, String> attributes) {

  /** A channel that declares neither a nature nor a single attribute. */
  static final ScenarioChannelConfig EMPTY = new ScenarioChannelConfig(null, Map.of());

  ScenarioChannelConfig {
    attributes = Map.copyOf(attributes);
  }

  /** Jackson hands {@code null} for a key the scenario omits; both fall back to declaring nothing. */
  @JsonCreator
  static ScenarioChannelConfig of(
      @JsonProperty("nature") @Nullable String nature,
      @JsonProperty("attributes") @Nullable Map<String, String> attributes) {
    return new ScenarioChannelConfig(nature, attributes == null ? Map.of() : attributes);
  }
}
