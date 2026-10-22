package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitepark.ies.aggregator.resolver.ResolverPath;
import com.sitepark.ies.aggregator.value.media.Image;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/** An image's {@code digitalSourceType} is read from the {@code media.metadata} fixture block. */
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
}
