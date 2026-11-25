package com.sitepark.ies.aggregator.testkit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitepark.ies.aggregator.port.ObjectTypeConfigProvider;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Answers the object-type configuration from the scenario.
 *
 * <p>In production the configuration belongs to an object: the adapter reads the type of the
 * object within the type of the group that offers it. The scenario stands for that with two blocks:
 *
 * <ul>
 *   <li>{@code objectTypes}, a map {@code type name -> config}: an object of the repository whose
 *       {@code objectType} the map names gets the configuration of its type. This is how a scenario
 *       configures the other objects an aggregation looks at - the children of a navigation, the
 *       pages of a site.
 *   <li>{@code objectType}: the configuration of the resource being aggregated. A scenario
 *       describes one resource, so it answers every object the {@code objectTypes} map does not
 *       cover.
 * </ul>
 *
 * <p>An object covered by neither block is not configured - the case a production aggregator also
 * has to survive.
 */
final class ScenarioObjectTypeConfigProvider implements ObjectTypeConfigProvider {

  private final ObjectMapper mapper;
  private final Repository repository;
  private final @Nullable Object rawObjectType;
  private final Map<?, ?> types;

  ScenarioObjectTypeConfigProvider(
      ObjectMapper mapper,
      Repository repository,
      @Nullable Object rawObjectType,
      @Nullable Object rawObjectTypes) {
    this.mapper = mapper;
    this.repository = repository;
    this.rawObjectType = rawObjectType;
    if (rawObjectTypes == null) {
      this.types = Map.of();
    } else if (rawObjectTypes instanceof Map<?, ?> map) {
      this.types = map;
    } else {
      throw new IllegalArgumentException(
          "'objectTypes' must be a map of type name to configuration: " + rawObjectTypes);
    }
  }

  @Override
  public <T> Optional<T> configuration(int objectId, Class<T> type) {
    String objectType = this.repository.objectTypeOfEntry(String.valueOf(objectId));
    Object raw = objectType == null ? null : this.types.get(objectType);
    if (raw == null) {
      raw = this.rawObjectType;
    }
    if (raw == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(this.mapper.convertValue(raw, type));
  }
}
