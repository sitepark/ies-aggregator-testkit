package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitepark.ies.aggregator.resolver.Resolver;
import com.sitepark.ies.aggregator.resolver.ResolverPath;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The id of a link target is read from the field as written, without the target having to be an
 * entry - production reads it without loading the target. A field that holds no id answers empty.
 */
class RepositoryResolverLinkTargetIdTest {

  private static String linkTargetId(Object field) {
    Map<String, Object> content = new HashMap<>();
    content.put("sp_userPool", field);
    Repository repository = new Repository(Map.of("1", Map.of("id", "1", "content", content)));
    Resolver page = ResolverPath.createRoot(path -> repository.resolver(path, "1"));
    return page.linkTargetId("sp_userPool");
  }

  @Test
  void answersTheIdOfALinkWrapperWhoseTargetIsNoEntry() {
    assertThat(linkTargetId(Map.of("link", "100560100000001500")))
        .as("a pool of the user repository is no entry of the scenario, its id is still answered")
        .isEqualTo("100560100000001500");
  }

  @Test
  void answersABareIdReference() {
    assertThat(linkTargetId("100560100000001500")).isEqualTo("100560100000001500");
  }

  @Test
  void answersTheFirstOfSeveralLinks() {
    assertThat(linkTargetId(Map.of("link", List.of("11", "12")))).isEqualTo("11");
  }

  @Test
  void answersTheReferencedIdOfAnIdReferenceWithOverrides() {
    assertThat(linkTargetId(Map.of("$ref", "11", "headline", "overridden"))).isEqualTo("11");
  }

  @Test
  void answersEmptyForAnInlineObject() {
    assertThat(linkTargetId(Map.of("sp_url", "https://example.com")))
        .as("a sub-information without a link holds no id")
        .isEmpty();
  }

  @Test
  void answersEmptyForAnEmptyLinkList() {
    assertThat(linkTargetId(Map.of("link", List.of()))).isEmpty();
  }

  @Test
  void answersEmptyForAnAbsentField() {
    Repository repository = new Repository(Map.of("1", Map.of("id", "1", "content", Map.of())));
    Resolver page = ResolverPath.createRoot(path -> repository.resolver(path, "1"));

    assertThat(page.linkTargetId("sp_userPool")).isEmpty();
  }
}
