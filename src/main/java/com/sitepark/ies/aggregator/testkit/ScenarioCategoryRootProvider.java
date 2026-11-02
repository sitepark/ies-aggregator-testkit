package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.port.CategoryRootProvider;
import com.sitepark.ies.aggregator.resolver.EntityResolver;
import java.util.List;

/**
 * Answers the category roots from the scenario's repository: every entry that is no group, has the
 * object type {@code category} and whose {@code sp_parent_category} holds no link.
 *
 * <p>That is the rule production applies to the whole installation, publication included: an
 * unpublished root is a root as well, so the scenario does not filter by it either.
 */
final class ScenarioCategoryRootProvider implements CategoryRootProvider {

  private final Repository repository;

  ScenarioCategoryRootProvider(Repository repository) {
    this.repository = repository;
  }

  @Override
  public List<EntityResolver> categoryRoots() {
    return this.repository.categoryRoots();
  }
}
