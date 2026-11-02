package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitepark.ies.aggregator.resolver.ResolverPath;
import com.sitepark.ies.aggregator.value.media.GenericMetadata;
import com.sitepark.ies.aggregator.value.media.Image;
import com.sitepark.ies.aggregator.value.media.ImageMetadata;
import com.sitepark.ies.aggregator.value.media.Media;
import com.sitepark.ies.aggregator.value.media.Video;
import java.time.Instant;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/** The media fixture block yields the metadata and kinds production can deliver. */
class RepositoryMediaProviderTest {

  private static @Nullable String digitalSourceType(Map<String, Object> metadata) {
    Repository repository =
        new Repository(Map.of("1", Map.of("id", "1", "media", Map.of("metadata", metadata))));
    var root = ResolverPath.createRoot(path -> repository.resolver(path, "1"));
    Image image = (Image) new RepositoryMediaProvider().resolveMedia(root).orElseThrow();
    return ((com.sitepark.ies.aggregator.value.media.ImageMetadata) image.metadata())
        .digitalSourceType();
  }

  @Test
  void readsTheDeclaredDigitalSourceType() {
    assertThat(digitalSourceType(Map.of("digitalSourceType", "trainedAlgorithmicMedia")))
        .isEqualTo("trainedAlgorithmicMedia");
  }

  @Test
  void isNullWhenTheFixtureDeclaresNone() {
    assertThat(digitalSourceType(Map.of("copyright", "© Site"))).isNull();
  }

  private static Media media(Map<String, Object> media) {
    Repository repository = new Repository(Map.of("1", Map.of("id", "1", "media", media)));
    var root = ResolverPath.createRoot(path -> repository.resolver(path, "1"));
    return new RepositoryMediaProvider().resolveMedia(root).orElseThrow();
  }

  @Test
  void readsTheLastModifiedOfAnImageAsEpochMillis() {
    Media image = media(Map.of("metadata", Map.of("lastModified", 1_700_000_000_123L)));
    assertThat(((ImageMetadata) image.metadata()).lastModified())
        .isEqualTo(Instant.ofEpochMilli(1_700_000_000_123L));
  }

  @Test
  void readsTheLastModifiedOfANonImage() {
    Media document =
        media(Map.of("kind", "document", "metadata", Map.of("lastModified", 1_700_000_000_000L)));
    assertThat(((GenericMetadata) document.metadata()).lastModified())
        .isEqualTo(Instant.ofEpochMilli(1_700_000_000_000L));
  }

  @Test
  void hasNoLastModifiedWhenTheFixtureDeclaresNone() {
    Media image = media(Map.of("metadata", Map.of("copyright", "© Site")));
    assertThat(((ImageMetadata) image.metadata()).lastModified()).isNull();
  }

  @Test
  void buildsAVideoWithItsDimensions() {
    Media media =
        media(
            Map.of(
                "kind",
                "video",
                "mimeType",
                "video/mp4",
                "width",
                1920,
                "height",
                1080,
                "metadata",
                Map.of("lastModified", 1_700_000_000_000L)));
    assertThat(media).isInstanceOf(Video.class);
    Video video = (Video) media;
    assertThat(video.width()).isEqualTo(1920);
    assertThat(video.height()).isEqualTo(1080);
    assertThat(video.mimeType()).isEqualTo("video/mp4");
    assertThat(video.metadata().lastModified()).isEqualTo(Instant.ofEpochMilli(1_700_000_000_000L));
  }

  @Test
  void keepsTheDimensionsOfAVideoWithoutThemAtZero() {
    Video video = (Video) media(Map.of("kind", "video"));
    assertThat(video.width()).isZero();
    assertThat(video.height()).isZero();
  }
}
