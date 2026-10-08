package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.port.Channel;
import com.sitepark.ies.aggregator.port.UrlLookup;
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
 * <p>A standalone medium is registered under its binary only - it has no row without a binary id -
 * so it never gets a page URL. An {@link UriTarget.ObjectTarget} on one is answered with the
 * download URL of the medium it publishes, exactly as the production channel does: linking a medium
 * internally is an ordinary thing for an editor to do, and the caller is not made to find out what
 * kind of object it is pointing at first.
 *
 * <p>Two things this must not go back to. An early version answered every target with a page URL:
 * that generosity hid a published bug, 322 media meta pages went out with {@code "url" =>
 * ".meta.php"} while every scenario stayed green. The version after it answered nothing at all for
 * a medium, which was right about the page URL and wrong about the link - production dropped the
 * whole link, and no scenario could show it.
 *
 * <p>As in production, the current channel answers a path and any other channel the full URL with
 * its host, so a link into another channel keeps pointing at that channel's site.
 */
final class ScenarioChannel implements Channel {

  private final int id;
  private final String name;
  private final @Nullable AccessRestriction accessRestriction;
  private final Repository repository;
  private final ScenarioChannelConfig config;
  private final boolean current;

  ScenarioChannel(
      int id,
      boolean current,
      String name,
      @Nullable AccessRestriction accessRestriction,
      Repository repository,
      ScenarioChannelConfig config) {
    this.id = id;
    this.name = name;
    this.accessRestriction = accessRestriction;
    this.repository = repository;
    this.config = config;
    this.current = current;
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
   * What the scenario's {@code channel} block names, and nothing more.
   *
   * <p>A scenario that names no nature leaves this empty, exactly as a publisher that declares none
   * does. Answering a default here would let a rule keyed to the public web pass without the
   * scenario ever having said which web it is.
   */
  @Override
  public Optional<String> nature() {
    return Optional.ofNullable(this.config.nature());
  }

  /** The {@code urlLookup} the scenario's {@code channel} block names; empty when it names none. */
  @Override
  public Optional<UrlLookup> urlLookup() {
    return Optional.ofNullable(this.config.urlLookup());
  }

  /** The attributes the scenario's {@code channel} block names; every other name stays empty. */
  @Override
  public Optional<String> attribute(String name) {
    return Optional.ofNullable(this.config.attributes().get(name));
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

  /**
   * Answers from the scenario, not with a blanket yes: an entry the scenario declares is published
   * unless it carries {@code "published": false}, and an id it does not declare is not published.
   *
   * <p>The former constant {@code true} was the more generous answer, and it hid a real defect for
   * as long as it stood — the IES adapter answered a constant {@code false}, so every caller of
   * this question got the opposite of the truth in production while every scenario stayed green.
   */
  @Override
  public boolean isPublished(int objectId) {
    return this.repository.isPublishedEntry(Integer.toString(objectId));
  }

  @Override
  public Optional<PlainUri> resolveUri(UriTarget target) {
    Optional<PlainUri> uri =
        switch (target) {
          case UriTarget.MediaTarget media ->
              Optional.of(mediaUri(media.objectId(), media.mediaId()));
          case UriTarget.ObjectTarget object -> this.objectUri(object.objectId());
        };
    return this.current ? uri.map(PlainUri::toAbsolutePathReference) : uri;
  }

  /**
   * The URL of an object: its page, or - for a standalone medium - the download URL of the medium
   * it publishes.
   *
   * <p>A medium still gets no page URL. It has none: it is published under its binary, and the
   * caller does not have to know that before asking. This mirrors the production channel, whose
   * object lookup falls back to the article's own binary for exactly this reason.
   */
  private Optional<PlainUri> objectUri(int objectId) {
    String id = Integer.toString(objectId);
    if (!this.repository.isMediaEntry(id)) {
      return Optional.of(PlainUri.of("https://example.com/object/" + objectId));
    }
    Integer mediaId = this.repository.mediaIdOfEntry(id);
    return mediaId == null ? Optional.empty() : Optional.of(mediaUri(objectId, mediaId));
  }

  private static PlainUri mediaUri(int objectId, int mediaId) {
    return PlainUri.of("https://example.com/media/" + objectId + "/" + mediaId);
  }
}
