package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.resolver.Resolver;
import com.sitepark.ies.aggregator.resolver.ResolverPath;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * In-memory, JSON-backed object repository used as the data source for aggregator integration
 * tests.
 *
 * <p>The scenario's {@code repository} block is a flat map {@code id -> entry}, mimicking a
 * normalized repository dump. Each entry keeps its structural metadata ({@code id}, an {@code
 * anchor}, a {@code parent} link, the {@code media} block, ...) at the top and its editable CMS
 * fields under a nested {@code content} node (see {@link RepositoryResolver}). Links between objects
 * (a linked media article, a media group {@code parent}, ...) are stored as string id references
 * that {@link RepositoryResolver} follows when navigating, thereby simulating the loading of
 * articles across link boundaries.
 *
 * <h2>Anchors</h2>
 *
 * <p>An entry's {@code anchor} is its stable, human-readable alias. All anchors are indexed on
 * construction, so an entry can be addressed by anchor as well as by id ({@link
 * ScenarioRootResolverFactory}). Anchors are unique in the CMS, hence a duplicate is rejected.
 *
 * <h2>Groups</h2>
 *
 * <p>Groups are ordinary entries carrying a {@code group} block, which holds the group-specific data
 * a {@link com.sitepark.ies.aggregator.resolver.GroupResolver} exposes (see {@link
 * RepositoryResolver}). The block is what classifies an entry as a group for a root lookup by id or
 * anchor — mirroring the production loaders, which tell pools and elements apart by their id type.
 */
final class Repository {

  /** Structural field holding an entry's anchor. */
  private static final String ANCHOR_KEY = "anchor";

  /** Structural block holding an entry's group data; its presence marks the entry as a group. */
  private static final String GROUP_KEY = "group";

  /** Structural block holding an entry's media asset; its presence marks the entry as a medium. */
  private static final String MEDIA_KEY = "media";

  /** Nested node holding an entry's editable CMS fields, where an upload sits. */
  private static final String CONTENT_KEY = "content";

  private final Map<String, Object> entries;
  private final Map<String, String> anchors;

  Repository(Map<String, Object> entries) {
    this.entries = entries;
    this.anchors = anchors(entries);
  }

  /** Indexes the anchors of all entries; anchors are unique, so a duplicate is a fixture error. */
  private static Map<String, String> anchors(Map<String, Object> entries) {
    Map<String, String> index = new HashMap<>();
    entries.forEach(
        (id, entry) -> {
          if (!(entry instanceof Map)) {
            return;
          }
          Object anchor = asObjectMap(entry).get(ANCHOR_KEY);
          if (anchor instanceof String name && !name.isEmpty()) {
            String previous = index.put(name, id);
            if (previous != null) {
              throw new IllegalArgumentException(
                  "Duplicate anchor '" + name + "' on entries " + previous + " and " + id);
            }
          }
        });
    return index;
  }

  /**
   * Returns the id of the entry carrying the given anchor.
   *
   * @param anchor the anchor to look up
   * @return the entry id, or {@code null} if no entry carries that anchor
   */
  @Nullable String idByAnchor(String anchor) {
    return this.anchors.get(anchor);
  }

  /**
   * Returns whether the entry with the given id exists and is a group (it carries a {@code group}
   * block).
   *
   * @param id the entry id
   */
  boolean isGroupEntry(String id) {
    Object entry = this.entries.get(id);
    return entry instanceof Map && asObjectMap(entry).get(GROUP_KEY) instanceof Map;
  }

  /**
   * Returns whether the entry with the given id exists and is a standalone medium - a resource whose
   * content <em>is</em> a media asset, carrying the {@code media} block at entry level.
   *
   * <p>A medium uploaded into a field of an article carries its block below that field and is
   * therefore not one: the article is an ordinary page that happens to hold a file.
   *
   * @param id the entry id
   */
  boolean isMediaEntry(String id) {
    Object entry = this.entries.get(id);
    return entry instanceof Map && asObjectMap(entry).get(MEDIA_KEY) instanceof Map;
  }

  /**
   * Returns the field path of the medium with the given asset id below the entry's {@code content},
   * or {@code null} if the entry carries no such upload.
   *
   * <p>Only {@code content} is searched, which is what tells an <em>embedded</em> medium from a
   * standalone one: an entry whose own node carries the {@code media} block <em>is</em> a medium
   * (see {@link #isMediaEntry(String)}), while an upload sits below a field of an ordinary page.
   *
   * <p>Nested map fields are followed and answered as a dot-separated path, the notation the
   * production navigation walks. A medium inside a list is not found — {@link
   * RepositoryResolver#resolve(String)} has no indexed access, and the field shape an upload sits
   * in is covered where it belongs, in the adapter.
   *
   * @param entryId the entry the asset was uploaded to
   * @param mediaId the asset id, as carried by the {@code media} block's {@code id}
   */
  @Nullable String embeddedMediaPath(String entryId, int mediaId) {
    Object entry = this.entries.get(entryId);
    if (!(entry instanceof Map)) {
      return null;
    }
    Object content = asObjectMap(entry).get(CONTENT_KEY);
    return content instanceof Map ? pathToMedia(asObjectMap(content), mediaId) : null;
  }

  private static @Nullable String pathToMedia(Map<String, Object> fields, int mediaId) {
    for (Map.Entry<String, Object> field : fields.entrySet()) {
      if (!(field.getValue() instanceof Map)) {
        continue;
      }
      Map<String, Object> node = asObjectMap(field.getValue());
      if (holdsMedia(node, mediaId)) {
        return field.getKey();
      }
      String nested = pathToMedia(node, mediaId);
      if (nested != null) {
        return field.getKey() + "." + nested;
      }
    }
    return null;
  }

  private static boolean holdsMedia(Map<String, Object> node, int mediaId) {
    return node.get(MEDIA_KEY) instanceof Map
        && asObjectMap(node.get(MEDIA_KEY)).get("id") instanceof Number id
        && id.intValue() == mediaId;
  }

  /**
   * Creates a resolver on the entry with the given id as the root of a fresh, standalone resolver
   * tree.
   *
   * @param id the entry id
   * @return the created root resolver, or {@code null} if no entry with that id exists
   */
  @Nullable RepositoryResolver createRoot(String id) {
    Object entry = this.entries.get(id);
    if (!(entry instanceof Map)) {
      return null;
    }
    return (RepositoryResolver)
        ResolverPath.createRoot(path -> new RepositoryResolver(path, asObjectMap(entry), this));
  }

  /**
   * Creates a resolver on the entry with the given id as a new root <em>within</em> an existing
   * navigation chain, so the navigation history is preserved.
   *
   * @param resolverPath the parent path the new root is appended to
   * @param id the entry id
   * @return the created root resolver, or {@code null} if no entry with that id exists
   */
  @Nullable RepositoryResolver enterRoot(ResolverPath resolverPath, String id) {
    Object entry = this.entries.get(id);
    if (!(entry instanceof Map)) {
      return null;
    }
    return (RepositoryResolver)
        resolverPath.enterRoot(path -> new RepositoryResolver(path, asObjectMap(entry), this));
  }

  /**
   * Returns a resolver positioned on the entry with the given id.
   *
   * @param id the entry id (the entry point of a scenario, e.g. the section source)
   * @throws IllegalArgumentException if no entry with that id exists
   */
  Resolver resolver(ResolverPath resolverPath, String id) {
    Object entry = this.entries.get(id);
    if (!(entry instanceof Map)) {
      throw new IllegalArgumentException("No repository entry with id: " + id);
    }
    return new RepositoryResolver(resolverPath, asObjectMap(entry), this);
  }

  /**
   * Turns a raw field value into a resolver: a {@link Map} becomes an inline object (an id
   * reference with placement overrides when it carries a {@code $ref} key), a {@link String} is
   * looked up as an id reference, anything else yields the empty resolver.
   *
   * <p>An inline object continues the {@code nodeKey} handed in, being part of the same entry; an
   * id reference is an entry of its own and starts a key of its own, as a followed link does in
   * production.
   */
  Resolver toResolver(ResolverPath resolverPath, @Nullable Object value, String assignedKey) {
    if (value instanceof Map) {
      Map<String, Object> node = asObjectMap(value);
      Object ref = node.get("$ref");
      if (ref instanceof String refId) {
        return new RepositoryResolver(resolverPath, overlay(refId, node), this, assignedKey);
      }
      return new RepositoryResolver(resolverPath, node, this, assignedKey);
    }
    if (value instanceof String id) {
      Object entry = this.entries.get(id);
      if (entry instanceof Map) {
        return ResolverPath.createRoot(
            path -> new RepositoryResolver(path, asObjectMap(entry), this));
      }
    }
    return Resolver.empty();
  }

  /** Loads the referenced entry and lays the remaining keys of {@code overrides} on top. */
  private Map<String, Object> overlay(String refId, Map<String, Object> overrides) {
    Map<String, Object> merged = new LinkedHashMap<>();
    Object base = this.entries.get(refId);
    if (base instanceof Map) {
      merged.putAll(asObjectMap(base));
    }
    overrides.forEach(
        (key, value) -> {
          if (!"$ref".equals(key)) {
            merged.put(key, value);
          }
        });
    return merged;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> asObjectMap(Object value) {
    return (Map<String, Object>) value;
  }
}
