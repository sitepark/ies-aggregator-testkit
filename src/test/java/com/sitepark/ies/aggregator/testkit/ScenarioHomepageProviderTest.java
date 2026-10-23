package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitepark.ies.aggregator.resolver.EntityResolver;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Home pages are the entries whose object type the {@code objectTypes} block marks as {@code home},
 * the rule production applies to the whole installation - nothing else counts as one.
 */
class ScenarioHomepageProviderTest {

  private static final Repository REPOSITORY =
      new Repository(
          Map.of(
              "10", Map.of("id", "10", "objectType", "home", "name", "Start"),
              "11", Map.of("id", "11", "objectType", "microsite-home", "name", "Microsite"),
              "12", Map.of("id", "12", "objectType", "news", "name", "News"),
              "20", Map.of("id", "20", "objectType", "home", "group", Map.of("rootSite", true))));

  private static final Map<String, Object> TYPES =
      Map.of(
          "home", Map.of("home", true),
          "microsite-home", Map.of("home", true),
          "news", Map.of("home", false));

  @Test
  void answersEveryEntryOfATypeMarkedAsHome() {
    ScenarioHomepageProvider provider = new ScenarioHomepageProvider(REPOSITORY, TYPES);

    assertThat(provider.homepages())
        .extracting(page -> page.entity().id())
        .containsExactlyInAnyOrder(10, 11);
  }

  @Test
  void eachHomePageIsARootOfItsOwn() {
    ScenarioHomepageProvider provider = new ScenarioHomepageProvider(REPOSITORY, TYPES);

    assertThat(provider.homepages()).allSatisfy(page -> assertThat(page.root()).isSameAs(page));
  }

  @Test
  void answersNoneForATypeNotMarkedAsHome() {
    ScenarioHomepageProvider provider =
        new ScenarioHomepageProvider(REPOSITORY, Map.of("news", Map.of("home", true)));

    assertThat(provider.homepages()).extracting(page -> page.entity().id()).containsExactly(12);
  }

  @Test
  void answersNoneWithoutTheObjectTypesBlock() {
    ScenarioHomepageProvider provider = new ScenarioHomepageProvider(REPOSITORY, null);

    assertThat(provider.homepages()).isEmpty();
  }

  @Test
  void leavesOutAGroupOfAHomeType() {
    ScenarioHomepageProvider provider =
        new ScenarioHomepageProvider(REPOSITORY, Map.of("home", Map.of("home", true)));

    assertThat(provider.homepages())
        .extracting(EntityResolver::entity)
        .extracting(entity -> entity.id())
        .containsExactly(10);
  }
}
