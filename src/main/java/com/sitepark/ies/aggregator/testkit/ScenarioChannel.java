package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.port.Channel;
import com.sitepark.ies.aggregator.value.AccessRestriction;
import com.sitepark.ies.aggregator.value.ResourcePathType;
import com.sitepark.ies.aggregator.value.uri.PlainUri;
import com.sitepark.ies.aggregator.value.uri.UriTarget;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * {@link Channel} that resolves a target to a deterministic URL built from its ids - <em>as far as
 * the publication would</em>: a media binary to a download URL of object and media id, a page to a
 * page URL of its object id.
 *
 * <p>A standalone medium is registered under its binary only, so {@link UriTarget.ObjectTarget}
 * stays unanswered for it and only {@link UriTarget.MediaTarget} resolves - exactly as the
 * production channel behaves, where a media resource has no row without a binary id. A scenario
 * that links to a medium therefore gets an empty internal link, which is the right answer even
 * though it looks like a testkit defect.
 *
 * <p>An earlier version answered every target alike. That generosity hid a published bug: 322 media
 * meta pages went out with {@code "url" => ".meta.php"} while every scenario stayed green.
 *
 * <p>The URLs are absolute, again like the production channel, which hands out {@code baseUrl +
 * path}. Reducing them to a path is the aggregator's job, not the channel's.
 */
final class ScenarioChannel implements Channel {

  private final int id;
  private final String name;
  private final @Nullable AccessRestriction accessRestriction;
  private final Repository repository;

  ScenarioChannel(
      int id, String name, @Nullable AccessRestriction accessRestriction, Repository repository) {
    this.id = id;
    this.name = name;
    this.accessRestriction = accessRestriction;
    this.repository = repository;
  }

  @Override
  public int id() {
    return this.id;
  }

  @Override
  public String name() {
    return this.name;
  }

  @Override
  public String encoding() {
    return "UTF-8";
  }

  /**
   * The restriction the scenario's {@code access} block names, for every object alike — a scenario
   * describes one resource.
   */
  @Override
  public Optional<AccessRestriction> accessRestriction(int objectId) {
    return Optional.ofNullable(this.accessRestriction);
  }

  /**
   * Always by path — the scheme every scenario describes.
   *
   * <p>A scenario that needs the id-based scheme would have to say so, and none does: the areas that
   * read it are not aggregated in Java yet.
   */
  @Override
  public ResourcePathType resourcePathType() {
    return ResourcePathType.URL;
  }

  @Override
  public boolean isPublished(int objectId) {
    return true;
  }

  @Override
  public Optional<PlainUri> resolveUri(UriTarget target) {
    return switch (target) {
      case UriTarget.MediaTarget media ->
          Optional.of(
              PlainUri.of("https://example.com/media/" + media.objectId() + "/" + media.mediaId()));
      case UriTarget.ObjectTarget object -> this.pageUri(object.objectId());
    };
  }

  /**
   * The page URL of an object, or empty for a standalone medium - that one is published under its
   * binary and has no page of its own.
   */
  private Optional<PlainUri> pageUri(int objectId) {
    if (this.repository.isMediaEntry(Integer.toString(objectId))) {
      return Optional.empty();
    }
    return Optional.of(PlainUri.of("https://example.com/object/" + objectId));
  }
}
