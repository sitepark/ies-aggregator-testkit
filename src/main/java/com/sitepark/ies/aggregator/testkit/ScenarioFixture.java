package com.sitepark.ies.aggregator.testkit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;

/**
 * The top-level blocks of the scenario file, for a project's own scenario-backed implementation of
 * a port the testkit does not know.
 *
 * <p>The testkit reads its own blocks ({@code options}, {@code repository}, …) itself. A port an
 * aggregator JAR declares and binds through an extension is unknown to it; the project stands in
 * for it with an implementation of its own, declared by an extension in its test sources, and that
 * implementation reads the block it needs from here by injection.
 */
public final class ScenarioFixture {

  private final ObjectMapper mapper;
  private final Map<String, Object> raw;

  ScenarioFixture(ObjectMapper mapper, Map<String, Object> raw) {
    this.mapper = mapper;
    this.raw = Map.copyOf(raw);
  }

  /**
   * Returns the block under a top-level key, or empty when the scenario has none.
   *
   * @param key the top-level key of the block
   * @param type what to read the block as
   */
  public <T> Optional<T> block(String key, TypeReference<T> type) {
    return Optional.ofNullable(this.raw.get(key))
        .map(block -> this.mapper.convertValue(block, type));
  }
}
