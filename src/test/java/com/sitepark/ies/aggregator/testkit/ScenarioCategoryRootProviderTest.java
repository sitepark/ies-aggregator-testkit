package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Category roots are the category entries whose {@code sp_parent_category} holds no link, as
 * production asks the store for the link - published or not, target present or not.
 */
class ScenarioCategoryRootProviderTest {

  private static Map<String, Object> category(String id, Map<String, Object> content) {
    return Map.of("id", id, "objectType", "category", "content", content);
  }

  private static final Repository REPOSITORY =
      new Repository(
          Map.of(
              "10", category("10", Map.of()),
              "11", category("11", Map.of("sp_parent_category", Map.of("link", "10"))),
              "12", category("12", Map.of("sp_parent_category", Map.of("link", "4711"))),
              "13",
                  Map.of(
                      "id",
                      "13",
                      "objectType",
                      "category",
                      "published",
                      false,
                      "content",
                      Map.of()),
              "14", Map.of("id", "14", "objectType", "news", "content", Map.of()),
              "15",
                  Map.of(
                      "id", "15", "objectType", "category", "group", Map.of(), "content", Map.of()),
              "16", category("16", Map.of("sp_parent_category", Map.of("sp_url", "x")))));

  @Test
  void answersEveryCategoryWithoutAParentLink() {
    assertThat(new ScenarioCategoryRootProvider(REPOSITORY).categoryRoots())
        .as("roots: no parent link, an unpublished one included; no child, no other type, no group")
        .extracting(root -> root.entity().id())
        .containsExactlyInAnyOrder(10, 13, 16);
  }

  @Test
  void aLinkToAnObjectTheScenarioLacksStillMakesAChild() {
    assertThat(new ScenarioCategoryRootProvider(REPOSITORY).categoryRoots())
        .extracting(root -> root.entity().id())
        .doesNotContain(12);
  }

  @Test
  void eachRootIsARootOfItsOwn() {
    assertThat(new ScenarioCategoryRootProvider(REPOSITORY).categoryRoots())
        .allSatisfy(root -> assertThat(root.root()).isSameAs(root));
  }

  @Test
  void answersNoneWithoutCategories() {
    Repository repository = new Repository(Map.of("1", Map.of("id", "1", "objectType", "news")));

    assertThat(new ScenarioCategoryRootProvider(repository).categoryRoots()).isEmpty();
  }
}
