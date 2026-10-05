package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.port.ResourceState;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * {@link ResourceState} of a scenario: one scenario is one resource, so the store lives as long as
 * the {@link ScenarioContext}, and an enclosing resource exists only while {@link
 * ScenarioContext#aggregateAsSection} plays the {@code sp:aggregator} tag.
 *
 * <p>No more generous than production: the enclosing resource is handed out as the tag hands it
 * out, the live map behind an unmodifiable view, and not at all on the paths without a tag.
 */
final class ScenarioResourceState implements ResourceState {

  private final Map<Class<?>, Object> attributes = new HashMap<>();
  private @Nullable Map<String, Object> enclosing;

  @Override
  public <T> T attribute(Class<T> key, Supplier<T> factory) {
    return key.cast(this.attributes.computeIfAbsent(key, k -> factory.get()));
  }

  @Override
  public Optional<Map<String, Object>> enclosing() {
    return Optional.ofNullable(this.enclosing).map(Collections::unmodifiableMap);
  }

  /** Called while the harness plays the tag, which seeds the resource the template has built. */
  void enterTag(Map<String, Object> resource) {
    this.enclosing = resource;
  }

  void exitTag() {
    this.enclosing = null;
  }
}
