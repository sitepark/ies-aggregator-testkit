package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The place-bound and the global object-type answers come from separate scenario blocks. */
class ScenarioObjectTypeConfigProviderTest {

  record Menu(boolean putInMenuDefault) {}

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  void answersTheGlobalDefinitionOfANamedType() {
    ScenarioObjectTypeConfigProvider provider =
        new ScenarioObjectTypeConfigProvider(
            MAPPER, null, Map.of("news", Map.of("putInMenuDefault", false)));

    assertThat(provider.typeConfiguration("news", Menu.class)).contains(new Menu(false));
  }

  @Test
  void answersEmptyForATypeTheScenarioDoesNotName() {
    ScenarioObjectTypeConfigProvider provider =
        new ScenarioObjectTypeConfigProvider(
            MAPPER, null, Map.of("news", Map.of("putInMenuDefault", false)));

    assertThat(provider.typeConfiguration("content", Menu.class)).isEmpty();
  }

  @Test
  void answersEmptyWithoutTheBlock() {
    ScenarioObjectTypeConfigProvider provider =
        new ScenarioObjectTypeConfigProvider(MAPPER, null, null);

    assertThat(provider.typeConfiguration("news", Menu.class)).isEmpty();
  }

  @Test
  void doesNotFeedTheGlobalAnswerFromTheObjectTypeBlock() {
    ScenarioObjectTypeConfigProvider provider =
        new ScenarioObjectTypeConfigProvider(MAPPER, Map.of("putInMenuDefault", true), null);

    assertThat(provider.typeConfiguration("news", Menu.class)).isEmpty();
    assertThat(provider.configuration(1, Menu.class)).contains(new Menu(true));
  }

  @Test
  void doesNotFeedThePlaceBoundAnswerFromTheGlobalBlock() {
    ScenarioObjectTypeConfigProvider provider =
        new ScenarioObjectTypeConfigProvider(
            MAPPER, null, Map.of("news", Map.of("putInMenuDefault", true)));

    assertThat(provider.configuration(1, Menu.class)).isEmpty();
  }

  @Test
  void rejectsABlockThatIsNotAMap() {
    assertThatThrownBy(() -> new ScenarioObjectTypeConfigProvider(MAPPER, null, "news"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
