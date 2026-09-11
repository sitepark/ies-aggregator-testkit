package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.resolver.EntityResolver;
import com.sitepark.ies.aggregator.resolver.GroupResolver;
import com.sitepark.ies.aggregator.resolver.ResolverPath;
import com.sitepark.ies.aggregator.resolver.RootResolverFactory;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * {@link RootResolverFactory} over the scenario {@link Repository}, so assemblers can read objects
 * that are not linked from the section source — addressed by id or by anchor.
 *
 * <p>Entities and groups are told apart the way the production loaders do it (they check the id type
 * of the loaded object): an entry is a group if it carries a {@code group} block, hence the {@code
 * createByGroup…} methods only find such entries and the {@code createByEntity…} methods only find
 * the others.
 *
 * <p>The null-safety contract of the port is kept: a lookup that finds nothing yields an empty
 * resolver instead of {@code null} — self-rooted for the standalone variants, carrying the given path
 * for the variants that continue a navigation chain.
 */
final class ScenarioRootResolverFactory implements RootResolverFactory {

  private final Repository repository;

  ScenarioRootResolverFactory(Repository repository) {
    this.repository = repository;
  }

  @Override
  public EntityResolver createByEntityId(int id) {
    return entityOrEmpty(this.createRoot(this.entityId(String.valueOf(id))));
  }

  @Override
  public EntityResolver createByEntityAnchor(String anchor) {
    return entityOrEmpty(this.createRoot(this.entityId(this.idByAnchor(anchor))));
  }

  @Override
  public EntityResolver createByEntityId(ResolverPath path, int id) {
    RepositoryResolver entity = this.enterRoot(path, this.entityId(String.valueOf(id)));
    return entity == null ? EntityResolver.empty(path) : entity;
  }

  @Override
  public GroupResolver createByGroupId(int id) {
    return groupOrEmpty(this.createRoot(this.groupId(String.valueOf(id))));
  }

  @Override
  public GroupResolver createByGroupAnchor(String anchor) {
    return groupOrEmpty(this.createRoot(this.groupId(this.idByAnchor(anchor))));
  }

  @Override
  public GroupResolver createByGroupId(ResolverPath path, int id) {
    RepositoryResolver group = this.enterRoot(path, this.groupId(String.valueOf(id)));
    return group == null ? GroupResolver.empty(path) : group;
  }

  private @Nullable String idByAnchor(String anchor) {
    Objects.requireNonNull(anchor, "anchor must not be null");
    return this.repository.idByAnchor(anchor);
  }

  /** Keeps the id only if it addresses an entity, so a group is never read as one. */
  private @Nullable String entityId(@Nullable String id) {
    return id != null && !this.repository.isGroupEntry(id) ? id : null;
  }

  /** Keeps the id only if it addresses a group, so an entity is never read as one. */
  private @Nullable String groupId(@Nullable String id) {
    return id != null && this.repository.isGroupEntry(id) ? id : null;
  }

  private @Nullable RepositoryResolver createRoot(@Nullable String id) {
    return id == null ? null : this.repository.createRoot(id);
  }

  private @Nullable RepositoryResolver enterRoot(ResolverPath path, @Nullable String id) {
    return id == null ? null : this.repository.enterRoot(path, id);
  }

  private static EntityResolver entityOrEmpty(@Nullable RepositoryResolver entity) {
    return entity == null ? EntityResolver.emptyRoot() : entity;
  }

  private static GroupResolver groupOrEmpty(@Nullable RepositoryResolver group) {
    return group == null ? GroupResolver.emptyRoot() : group;
  }
}
