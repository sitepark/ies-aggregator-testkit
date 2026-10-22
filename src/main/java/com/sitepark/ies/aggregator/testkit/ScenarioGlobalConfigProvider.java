package com.sitepark.ies.aggregator.testkit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitepark.ies.aggregator.port.GlobalConfigProvider;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Answers the installation's global configuration from the scenario's {@code global} block, a map
 * {@code entry name -> configuration}.
 *
 * <p>Only an entry the scenario names is answered; everything else, and a scenario without the
 * block, answers empty. That is deliberately not more generous than production: a feature whose
 * global configuration is absent is off, and a scenario has to switch it on explicitly.
 */
final class ScenarioGlobalConfigProvider implements GlobalConfigProvider {

  private final ObjectMapper mapper;
  private final Map<?, ?> entries;

  ScenarioGlobalConfigProvider(ObjectMapper mapper, @Nullable Object rawGlobal) {
    this.mapper = mapper;
    if (rawGlobal == null) {
      this.entries = Map.of();
    } else if (rawGlobal instanceof Map<?, ?> map) {
      this.entries = map;
    } else {
      throw new IllegalArgumentException(
          "'global' must be a map of entry name to configuration: " + rawGlobal);
    }
  }

  @Override
  public <T> Optional<T> configuration(String name, Class<T> type) {
    Object raw = this.entries.get(name);
    if (raw == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(this.mapper.convertValue(raw, type));
  }
}
