package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitepark.ies.aggregator.resolver.EntityResolver;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The entities of an object type are what the {@code sp:collection} of the legacy answers during a
 * generation: entries of the type that are no group, published in the channel being generated, in
 * the order of the scenario.
 */
class ScenarioObjectTypeEntitiesTest {

  private static final Repository REPOSITORY = repository();

  private static Repository repository() {
    Map<String, Object> entries = new LinkedHashMap<>();
    entries.put("30", Map.of("id", "30", "objectType", "notice", "name", "Third"));
    entries.put("10", Map.of("id", "10", "objectType", "notice", "name", "First"));
    entries.put("11", Map.of("id", "11", "objectType", "notice", "published", false));
    entries.put("12", Map.of("id", "12", "objectType", "notice", "published", "primary"));
    entries.put("13", Map.of("id", "13", "objectType", "news"));
    entries.put(
        "20", Map.of("id", "20", "objectType", "notice", "group", Map.of("rootSite", true)));
    return new Repository(entries);
  }

  private final ScenarioObjectTypeEntities entities = new ScenarioObjectTypeEntities(REPOSITORY);

  private static java.util.List<Integer> ids(java.util.List<EntityResolver> found) {
    return found.stream().map(entity -> entity.entity().id()).toList();
  }

  @Test
  void answersTheEntriesOfTheTypeInTheOrderOfTheScenario() {
    assertThat(ids(this.entities.of("notice"))).containsExactly(30, 10);
  }

  @Test
  void leavesOutAnEntryTheCurrentChannelDoesNotPublish() {
    assertThat(ids(this.entities.of("notice"))).doesNotContain(11, 12);
  }

  @Test
  void leavesOutAGroupOfThatType() {
    assertThat(ids(this.entities.of("notice"))).doesNotContain(20);
  }

  @Test
  void answersOnlyTheAskedType() {
    assertThat(ids(this.entities.of("news"))).containsExactly(13);
  }

  @Test
  void answersNoneForATypeNobodyUses() {
    assertThat(this.entities.of("planingTheme")).isEmpty();
  }

  @Test
  void eachEntityIsARootOfItsOwn() {
    assertThat(this.entities.of("notice"))
        .allSatisfy(entity -> assertThat(entity.root()).isSameAs(entity));
  }
}
