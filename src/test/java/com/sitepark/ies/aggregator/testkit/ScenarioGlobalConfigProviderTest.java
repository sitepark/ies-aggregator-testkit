package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The global configuration is answered only from an explicit {@code global} block. */
class ScenarioGlobalConfigProviderTest {

  record Labeling(boolean enabled) {}

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  void answersADeclaredEntry() {
    ScenarioGlobalConfigProvider provider =
        new ScenarioGlobalConfigProvider(MAPPER, Map.of("labeling", Map.of("enabled", true)));

    assertThat(provider.configuration("labeling", Labeling.class)).contains(new Labeling(true));
  }

  @Test
  void answersEmptyForAnEntryTheScenarioDoesNotName() {
    ScenarioGlobalConfigProvider provider =
        new ScenarioGlobalConfigProvider(MAPPER, Map.of("labeling", Map.of("enabled", true)));

    assertThat(provider.configuration("other", Labeling.class)).isEmpty();
  }

  @Test
  void answersEmptyWithoutTheBlock() {
    ScenarioGlobalConfigProvider provider = new ScenarioGlobalConfigProvider(MAPPER, null);

    assertThat(provider.configuration("labeling", Labeling.class)).isEmpty();
  }

  @Test
  void rejectsABlockThatIsNotAMap() {
    assertThatThrownBy(() -> new ScenarioGlobalConfigProvider(MAPPER, "labeling"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
