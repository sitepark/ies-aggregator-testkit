package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.port.ModuleDependencies;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link ModuleDependencies} of a scenario: records every dependency on a module the scenario's
 * {@code modules} block installs, so a test can ask what the publisher would have stored.
 *
 * <p>No more generous than production: an id the environment does not know is dropped there, so it
 * is dropped here - a scenario without a {@code modules} block has no module installed. The same
 * module reported twice is recorded once, as the publisher keeps one dependency per module.
 */
final class ScenarioModuleDependencies implements ModuleDependencies {

  private final Map<String, String> installed;
  private final Map<String, ModuleDependency> reported = new LinkedHashMap<>();

  ScenarioModuleDependencies(Map<String, String> installed) {
    this.installed = Map.copyOf(installed);
  }

  @Override
  public void dependsOn(String moduleId) {
    this.version(moduleId);
  }

  @Override
  public Optional<String> version(String moduleId) {
    String version = this.installed.get(moduleId);
    if (version == null) {
      return Optional.empty();
    }
    this.reported.putIfAbsent(moduleId, new ModuleDependency(moduleId, version));
    return Optional.of(version);
  }

  List<ModuleDependency> reported() {
    return List.copyOf(this.reported.values());
  }
}
