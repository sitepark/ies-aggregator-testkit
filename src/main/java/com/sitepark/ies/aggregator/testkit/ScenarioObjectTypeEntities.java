package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.port.ObjectTypeEntities;
import com.sitepark.ies.aggregator.resolver.EntityResolver;
import java.util.List;
import java.util.Set;

/**
 * Answers the entities of an object type from the scenario's repository: every entry that is no
 * group and whose {@code objectType} is the asked one, in the order the scenario lists them, as long
 * as it is published in the current channel.
 *
 * <p>That is what the {@code sp:collection} of the legacy answers during a generation: the search
 * runs restricted to the channel being generated, so an entry the scenario marks {@code
 * "published": false}, or {@code "published": "primary"}, is not part of the answer - not even for
 * a caller that, like the planing themes, never asks for publication itself. Unlike the home pages
 * and the category roots, which production answers for the whole installation, an object type is
 * always asked in the course of a generation.
 */
final class ScenarioObjectTypeEntities implements ObjectTypeEntities {

  private final Repository repository;

  ScenarioObjectTypeEntities(Repository repository) {
    this.repository = repository;
  }

  @Override
  public List<EntityResolver> of(String objectType) {
    return this.repository.entitiesOfTypes(Set.of(objectType)).stream()
        .filter(
            entity ->
                this.repository.isPublishedEntry(Integer.toString(entity.entity().id()), true))
        .toList();
  }
}
