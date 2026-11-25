package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * An object of the repository gets the configuration of its type from the {@code objectTypes}
 * block; whatever that block does not cover is answered by the resource's {@code objectType} block.
 */
class ScenarioObjectTypeConfigProviderTest {

  record Menu(boolean putInMenuDefault) {}

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final Repository REPOSITORY =
      new Repository(
          Map.of(
              "1", Map.of("id", "1", "objectType", "news"),
              "2", Map.of("id", "2", "objectType", "content"),
              "3", Map.of("id", "3"),
              "4", Map.of("id", "4", "objectType", "news", "group", Map.of())));

  private static ScenarioObjectTypeConfigProvider provider(Object objectType, Object objectTypes) {
    return new ScenarioObjectTypeConfigProvider(MAPPER, REPOSITORY, objectType, objectTypes);
  }

  @Test
  void anObjectGetsTheConfigurationOfItsType() {
    ScenarioObjectTypeConfigProvider provider =
        provider(null, Map.of("news", Map.of("putInMenuDefault", false)));

    assertThat(provider.configuration(1, Menu.class))
        .as("the news object should get the configuration of the type news")
        .contains(new Menu(false));
  }

  @Test
  void theTypeOfTheObjectWinsOverTheResourceBlock() {
    ScenarioObjectTypeConfigProvider provider =
        provider(
            Map.of("putInMenuDefault", true), Map.of("news", Map.of("putInMenuDefault", false)));

    assertThat(provider.configuration(1, Menu.class)).contains(new Menu(false));
  }

  @Test
  void anObjectOfATypeTheMapDoesNotNameGetsTheResourceBlock() {
    ScenarioObjectTypeConfigProvider provider =
        provider(
            Map.of("putInMenuDefault", true), Map.of("news", Map.of("putInMenuDefault", false)));

    assertThat(provider.configuration(2, Menu.class)).contains(new Menu(true));
    assertThat(provider.configuration(3, Menu.class)).contains(new Menu(true));
    assertThat(provider.configuration(99, Menu.class)).contains(new Menu(true));
  }

  @Test
  void aGroupHasNoObjectType() {
    ScenarioObjectTypeConfigProvider provider =
        provider(null, Map.of("news", Map.of("putInMenuDefault", false)));

    assertThat(provider.configuration(4, Menu.class)).isEmpty();
  }

  @Test
  void anObjectCoveredByNeitherBlockIsNotConfigured() {
    assertThat(provider(null, null).configuration(1, Menu.class)).isEmpty();
    assertThat(
            provider(null, Map.of("news", Map.of("putInMenuDefault", false)))
                .configuration(2, Menu.class))
        .isEmpty();
  }

  @Test
  void rejectsABlockThatIsNotAMap() {
    assertThatThrownBy(() -> provider(null, "news")).isInstanceOf(IllegalArgumentException.class);
  }
}
