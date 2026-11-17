package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitepark.ies.aggregator.container.ContainerEntry;
import com.sitepark.ies.aggregator.container.ContainerNode;
import com.sitepark.ies.aggregator.container.ContainerSection;
import com.sitepark.ies.aggregator.resolver.Resolver;
import com.sitepark.ies.aggregator.resolver.ResolverPath;
import com.sitepark.ies.aggregator.testkit.probe.ContainerProbeAggregator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The scenario states the tree of a container and the testkit hands it out as stated: to the scope
 * it is addressed to and to no other, structurally, with nothing dropped, repaired or numbered.
 */
class ScenarioContainerHierarchyTest {

  private static final ScenarioLayout LAYOUT =
      new ScenarioLayout("content", "ROOT", "main", "main");
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final String FIELD = "sp_main_iterate";

  private static final Map<String, Object> ENTRIES =
      Map.of(
          "1000", Map.of("id", 1000),
          "2001", Map.of("id", 2001, "content", Map.of("sp_headline", "First")),
          "2003", Map.of("id", 2003));

  private static Repository repository() {
    return new Repository(ENTRIES);
  }

  private static Resolver entry(Repository repository, String id) {
    return ResolverPath.createRoot(path -> repository.resolver(path, id));
  }

  private static ScenarioContainerHierarchy hierarchy(Repository repository, Object block) {
    return new ScenarioContainerHierarchy(repository, "1000", block);
  }

  @Test
  void handsTheTreeOfTheSourceUnderTheListFieldAlone() {
    Repository repository = repository();
    ScenarioContainerHierarchy hierarchy =
        hierarchy(
            repository,
            Map.of(
                FIELD,
                List.of(Map.of("entry", "2001", "index", 4, "rule", Map.of("newSection", true)))));

    List<ContainerNode<Resolver>> tree = hierarchy.of(entry(repository, "1000"), FIELD);

    assertThat(tree).hasSize(1);
    ContainerEntry<Resolver> first = (ContainerEntry<Resolver>) tree.get(0);
    assertThat(first.index()).isEqualTo(4);
    assertThat(first.rule().newSection()).isTrue();
    assertThat(first.entry().value("sp_headline").asString()).isEqualTo("First");
  }

  @Test
  void addressesAnotherScopeByItsEntryIdAndNeverHandsItTheTreeOfTheSource() {
    Repository repository = repository();
    ScenarioContainerHierarchy hierarchy =
        hierarchy(
            repository,
            Map.of(
                FIELD,
                List.of(Map.of("entry", "2001", "index", 0)),
                "2003/" + FIELD,
                List.of(Map.of("entry", "2001", "index", 7))));

    assertThat(hierarchy.of(entry(repository, "2003"), FIELD))
        .singleElement()
        .isInstanceOfSatisfying(
            ContainerEntry.class, nested -> assertThat(nested.index()).isEqualTo(7));
    assertThat(hierarchy.of(entry(repository, "2001"), FIELD))
        .as("a scope the scenario states no tree for has no entries")
        .isEmpty();
    assertThat(hierarchy.of(entry(repository, "1000"), "sp_other"))
        .as("another list field of a known scope has none either")
        .isEmpty();
  }

  @Test
  void answersAnEmptyListWithoutABlock() {
    Repository repository = repository();

    assertThat(hierarchy(repository, null).of(entry(repository, "1000"), FIELD)).isEmpty();
  }

  @Test
  void answersAnEmptyListForAScopeThatIsNoRepositoryEntry() {
    Repository repository = repository();
    ScenarioContainerHierarchy hierarchy =
        hierarchy(repository, Map.of(FIELD, List.of(Map.of("entry", "2001", "index", 0))));

    assertThat(hierarchy.of(Resolver.empty(), FIELD)).isEmpty();
  }

  @Test
  void keepsASectionAsStatedWithoutChildrenAndAnEntryWithoutARule() {
    Repository repository = repository();
    ScenarioContainerHierarchy hierarchy =
        hierarchy(
            repository,
            Map.of(
                FIELD,
                List.of(
                    Map.of(
                        "section",
                        Map.of(
                            "outputType",
                            "accordion",
                            "implicit",
                            true,
                            "ordinal",
                            3,
                            "opener",
                            Map.of("entry", "2003", "index", 1))))));

    ContainerSection<Resolver> section =
        (ContainerSection<Resolver>) hierarchy.of(entry(repository, "1000"), FIELD).get(0);

    assertThat(section.outputType()).isEqualTo("accordion");
    assertThat(section.implicit()).isTrue();
    assertThat(section.ordinal()).isEqualTo(3);
    assertThat(section.children()).as("an empty section is not dropped here").isEmpty();
    assertThat(section.opener().rule()).as("unconfigured stays unconfigured").isNull();
  }

  @Test
  void failsOnAnEntryTheRepositoryDoesNotHold() {
    Repository repository = repository();

    assertThatThrownBy(
            () -> hierarchy(repository, Map.of(FIELD, List.of(Map.of("entry", "9", "index", 0)))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("9");
  }

  @Test
  void failsOnAMalformedTreeInsteadOfRepairingIt() {
    Repository repository = repository();

    assertThatThrownBy(() -> hierarchy(repository, Map.of(FIELD, List.of(Map.of("index", 0)))))
        .as("an entry without its key")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                hierarchy(
                    repository,
                    Map.of(FIELD, List.of(Map.of("entry", "2001", "index", 0, "id", "x")))))
        .as("an unknown key")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("id");
    assertThatThrownBy(() -> hierarchy(repository, Map.of(FIELD, "2001")))
        .as("a tree that is no list")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void reachesAnAggregatorThroughTheScenario() throws JsonProcessingException {
    ScenarioContext context =
        ScenarioContext.load(
            "testkit/container.json", LAYOUT, "com.sitepark.ies.aggregator.testkit.probe");

    JsonNode container =
        JSON.readTree(context.aggregate(context.aggregator(ContainerProbeAggregator.class)))
            .path("container");

    assertThat(container.path("n0").asText()).isEqualTo("First@0/content");
    assertThat(container.path("n1").asText())
        .isEqualTo("section-2:section:ordinal=1:implicit=true:opener=-");
    assertThat(container.path("n2").asText()).isEqualTo("Second@1/unconfigured");
    assertThat(container.path("n3").asText())
        .isEqualTo("section-3:accordion:ordinal=2:implicit=false:opener=Opener@2/opens1");
  }
}
