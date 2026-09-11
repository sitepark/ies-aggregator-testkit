package com.sitepark.ies.aggregator.testkit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitepark.ies.aggregator.port.ObjectTypeConfigProvider;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Answers the object-type configuration from the scenario's {@code objectType} block.
 *
 * <p>A scenario describes one resource, so it configures one object type and the object id is
 * ignored. An absent block means the type is not configured at all — the case a production
 * aggregator also has to survive.
 */
final class ScenarioObjectTypeConfigProvider implements ObjectTypeConfigProvider {

  private final ObjectMapper mapper;
  private final @Nullable Object rawObjectType;

  ScenarioObjectTypeConfigProvider(ObjectMapper mapper, @Nullable Object rawObjectType) {
    this.mapper = mapper;
    this.rawObjectType = rawObjectType;
  }

  @Override
  public <T> Optional<T> configuration(int objectId, Class<T> type) {
    if (this.rawObjectType == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(this.mapper.convertValue(this.rawObjectType, type));
  }
}
