package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitepark.ies.aggregator.resolver.EntityResolver;
import com.sitepark.ies.aggregator.resolver.Resolver;
import com.sitepark.ies.aggregator.resolver.ResolverPath;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * A link field is followed only when it names an entry, as {@code
 * BaseInformationVOResolver.resolveLink} follows one only when it carries a link id whose article
 * loads. Following anything else would make the harness more generous than production - a port
 * reading the link from the wrong level got an entity with id {@code 0} here and none in production.
 */
class RepositoryResolverLinkTest {

  private static final Map<String, Object> TARGET = Map.of("id", "2", "objectType", "news");

  private static EntityResolver follow(Object field) {
    Repository repository =
        new Repository(
            Map.of("1", Map.of("id", "1", "content", Map.of("sp_link", field)), "2", TARGET));
    Resolver page = ResolverPath.createRoot(path -> repository.resolver(path, "1"));
    return page.resolveLink("sp_link");
  }

  @Test
  void followsAnIdReference() {
    assertThat(follow("2").isEmpty()).isFalse();
  }

  @Test
  void followsALinkWrapper() {
    assertThat(follow(Map.of("link", "2")).isEmpty()).isFalse();
  }

  @Test
  void followsAnIdReferenceWithOverrides() {
    assertThat(follow(Map.of("$ref", "2", "headline", "overridden")).isEmpty()).isFalse();
  }

  @Test
  void doesNotFollowAnInlineObject() {
    assertThat(follow(Map.of("sp_linkType", "external", "sp_url", "https://example.com")).isEmpty())
        .as("a sub-information without a link is no link target")
        .isTrue();
  }

  @Test
  void doesNotFollowAnUnknownId() {
    assertThat(follow("4711").isEmpty()).as("an article that does not load").isTrue();
  }

  @Test
  void doesNotFollowALinkWrapperAroundAnInlineObject() {
    assertThat(follow(Map.of("link", Map.of("sp_url", "https://example.com"))).isEmpty()).isTrue();
  }
}
