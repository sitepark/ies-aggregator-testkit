package com.sitepark.ies.aggregator.testkit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitepark.ies.aggregator.port.ObjectTypeConfigProvider;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Answers the object-type configuration from the scenario.
 *
 * <p>{@link #configuration(int, Class)} - what applies to one object at its place - reads the
 * {@code objectType} block. A scenario describes one resource, so it configures one object type and
 * the object id is ignored. An absent block means the type is not configured at all - the case a
 * production aggregator also has to survive.
 *
 * <p>{@link #typeConfiguration(String, Class)} - the global definition of a type, independent of
 * where an object sits - reads the {@code objectTypes} block, a map {@code type name -> config}. The
 * two answers deliberately share no data: in production they come from different places (the group
 * type's offering versus the global type definition), so a scenario that wants both to agree has to
 * declare both. A type the block does not name, or a scenario without the block, answers empty.
 */
final class ScenarioObjectTypeConfigProvider implements ObjectTypeConfigProvider {

  private final ObjectMapper mapper;
  private final @Nullable Object rawObjectType;
  private final Map<?, ?> globalTypes;

  ScenarioObjectTypeConfigProvider(
      ObjectMapper mapper, @Nullable Object rawObjectType, @Nullable Object rawObjectTypes) {
    this.mapper = mapper;
    this.rawObjectType = rawObjectType;
    if (rawObjectTypes == null) {
      this.globalTypes = Map.of();
    } else if (rawObjectTypes instanceof Map<?, ?> map) {
      this.globalTypes = map;
    } else {
      throw new IllegalArgumentException(
          "'objectTypes' must be a map of type name to configuration: " + rawObjectTypes);
    }
  }

  @Override
  public <T> Optional<T> configuration(int objectId, Class<T> type) {
    if (this.rawObjectType == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(this.mapper.convertValue(this.rawObjectType, type));
  }

  @Override
  public <T> Optional<T> typeConfiguration(String objectType, Class<T> type) {
    Object raw = this.globalTypes.get(objectType);
    if (raw == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(this.mapper.convertValue(raw, type));
  }
}
