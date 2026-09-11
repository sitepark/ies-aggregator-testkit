package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.port.MediaProvider;
import com.sitepark.ies.aggregator.resolver.EntityResolver;
import com.sitepark.ies.aggregator.resolver.Resolver;
import com.sitepark.ies.aggregator.value.ResolvedValue;
import com.sitepark.ies.aggregator.value.media.ColorSwatch;
import com.sitepark.ies.aggregator.value.media.Document;
import com.sitepark.ies.aggregator.value.media.FocalPoint;
import com.sitepark.ies.aggregator.value.media.GenericMetadata;
import com.sitepark.ies.aggregator.value.media.Hash;
import com.sitepark.ies.aggregator.value.media.HashAlgorithm;
import com.sitepark.ies.aggregator.value.media.Image;
import com.sitepark.ies.aggregator.value.media.ImageMetadata;
import com.sitepark.ies.aggregator.value.media.Media;
import com.sitepark.ies.aggregator.value.media.Origin;
import com.sitepark.ies.aggregator.value.media.UnclassifiedMedia;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * {@link MediaProvider} that builds a {@link Media} asset from the reserved {@code media} block of
 * the given resolver.
 *
 * <p>The block carries the binary asset facts (mime type, dimensions, an optional {@code
 * colorSwatch}, ...) and a nested {@code metadata} object (copyright, alternative text, ...) — the
 * same nesting as the legacy {@code binary.media} path. Only {@link Image} media is supported for
 * now, which is all the art-direction scenarios need. A missing {@code media} block yields {@link
 * Optional#empty()}, simulating a link that resolves to no media.
 *
 * <p>The asset's object id is taken from the resolver's scope root, mirroring production: for an
 * uploaded image that is the enclosing article, for a linked media article the media article itself
 * (the link crossing opens a new scope).
 */
final class RepositoryMediaProvider implements MediaProvider {

  @Override
  public Optional<Media> resolveMedia(Resolver resolver) {
    Resolver media = resolver.resolve("media");
    if (media.isEmpty()) {
      return Optional.empty();
    }

    Resolver metadata = media.resolve("metadata");
    String kind = media.value("kind").asString("image");
    if (!"image".equals(kind)) {
      return Optional.of(nonImage(kind, resolver, media, metadata));
    }
    ImageMetadata imageMetadata =
        new ImageMetadata(
            nullableString(metadata, "alternativeText"),
            nullableString(metadata, "copyright"),
            nullableString(metadata, "title"),
            nullableString(metadata, "description"),
            null,
            focalPoint(metadata.resolve("focalpoint")));

    return Optional.of(
        new Image(
            objectId(resolver),
            media.value("id").asInt(0),
            media.value("filename").asString(""),
            media.value("originFilename").asString(""),
            media.value("mimeType").asString(""),
            media.value("fileSize").asLong(0L),
            new Hash(HashAlgorithm.SHA_256, media.value("hash").asString("")),
            imageMetadata,
            origin(media.resolve("origin")),
            media.value("width").asInt(0),
            media.value("height").asInt(0),
            colorSwatch(media.resolve("colorSwatch"))));
  }

  /**
   * The media kinds that are not an image, chosen by the scenario's {@code kind} key.
   *
   * <p>{@code unclassified} is the one the store itself cannot sort — a PostScript file, say. It
   * carries no kind-specific data, and a scenario needs it to show that a download link is built
   * from the shared fields alone.
   */
  private static Media nonImage(String kind, Resolver resolver, Resolver media, Resolver metadata) {
    GenericMetadata genericMetadata =
        new GenericMetadata(
            nullableString(metadata, "alternativeText"),
            nullableString(metadata, "copyright"),
            nullableString(metadata, "title"),
            nullableString(metadata, "description"),
            null);
    int objectId = objectId(resolver);
    int id = media.value("id").asInt(0);
    String filename = media.value("filename").asString("");
    String originFilename = media.value("originFilename").asString("");
    String mimeType = media.value("mimeType").asString("");
    long fileSize = media.value("fileSize").asLong(0L);
    Hash hash = new Hash(HashAlgorithm.SHA_256, media.value("hash").asString(""));
    Origin mediaOrigin = origin(media.resolve("origin"));
    return switch (kind) {
      case "document" ->
          new Document(
              objectId,
              id,
              filename,
              originFilename,
              mimeType,
              fileSize,
              hash,
              genericMetadata,
              mediaOrigin,
              nullableString(media, "extractedContent"));
      case "unclassified" ->
          new UnclassifiedMedia(
              objectId,
              id,
              filename,
              originFilename,
              mimeType,
              fileSize,
              hash,
              genericMetadata,
              mediaOrigin);
      default -> throw new IllegalArgumentException("unknown media kind in scenario: " + kind);
    };
  }

  /** The id of the object the media belongs to, or {@code 0} if the scope root is not an entity. */
  private static int objectId(Resolver resolver) {
    return resolver.root() instanceof EntityResolver entity ? entity.entity().id() : 0;
  }

  private static @Nullable String nullableString(Resolver resolver, String key) {
    ResolvedValue value = resolver.value(key);
    return value.isEmpty() ? null : value.asString();
  }

  /**
   * The color swatch of the {@code colorSwatch} block, or {@code null} if the block is absent or
   * names no vibrant color — mirroring a media asset from which no colors were extracted.
   */
  private static @Nullable ColorSwatch colorSwatch(Resolver resolver) {
    String vibrantColor = nullableString(resolver, "vibrantColor");
    return vibrantColor == null ? null : new ColorSwatch(vibrantColor);
  }

  /**
   * The provenance of the {@code origin} block, or {@code null} if the block is absent or does not
   * name both a system and an id — mirroring an asset that stems from no external system.
   */
  private static @Nullable Origin origin(Resolver resolver) {
    String system = nullableString(resolver, "system");
    String id = nullableString(resolver, "id");
    return system == null || id == null ? null : new Origin(system, id);
  }

  private static FocalPoint focalPoint(Resolver resolver) {
    if (resolver.isEmpty()) {
      return FocalPoint.CENTER;
    }
    return new FocalPoint(resolver.value("x").asFloat(0.5f), resolver.value("y").asFloat(0.5f));
  }
}
