package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.port.HomepageProvider;
import com.sitepark.ies.aggregator.resolver.EntityResolver;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Answers the home pages from the scenario's repository: every entry that is no group and whose
 * {@code objectType} the {@code objectTypes} block marks with {@code "home": true}.
 *
 * <p>That is the rule production applies to the whole installation, so a scenario has to declare
 * both the type definition and the entries; a type the block does not mark, or a scenario without
 * the block, yields no home page.
 */
final class ScenarioHomepageProvider implements HomepageProvider {

  private final Repository repository;
  private final Set<String> homeTypes;

  ScenarioHomepageProvider(Repository repository, @Nullable Object rawObjectTypes) {
    this.repository = repository;
    this.homeTypes = homeTypes(rawObjectTypes);
  }

  private static Set<String> homeTypes(@Nullable Object rawObjectTypes) {
    if (!(rawObjectTypes instanceof Map<?, ?> types)) {
      return Set.of();
    }
    Set<String> home = new HashSet<>();
    types.forEach(
        (name, config) -> {
          if (config instanceof Map<?, ?> definition
              && Boolean.TRUE.equals(definition.get("home"))) {
            home.add(String.valueOf(name));
          }
        });
    return Set.copyOf(home);
  }

  @Override
  public List<EntityResolver> homepages() {
    return this.homeTypes.isEmpty() ? List.of() : this.repository.entitiesOfTypes(this.homeTypes);
  }
}
