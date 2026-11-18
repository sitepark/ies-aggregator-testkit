package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitepark.ies.aggregator.resolver.Resolver;
import com.sitepark.ies.aggregator.resolver.ResolverPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@code resolveList} answers the elements of a list field and nothing else, as {@code
 * BaseInformationVOResolver.resolveList} does: it returns the empty list unless the field is a
 * {@code ListInformationVO}, and keeps only its {@code ListItemInformationVO} entries.
 */
class RepositoryResolverListTest {

  private static List<Resolver> listOf(Object field) {
    Repository repository =
        new Repository(
            Map.of(
                "1",
                Map.of("id", "1", "content", Map.of("sp_items", field)),
                "2",
                Map.of("id", "2", "objectType", "news")));
    Resolver page = ResolverPath.createRoot(path -> repository.resolver(path, "1"));
    return page.resolveList("sp_items");
  }

  @Test
  void listsTheObjectsOfAListField() {
    List<Resolver> items = listOf(List.of(Map.of("sp_title", "a"), Map.of("sp_title", "b")));

    assertThat(items)
        .extracting(item -> item.value("sp_title").asString(""))
        .containsExactly("a", "b");
  }

  @Test
  void answersNoElementForAFieldHoldingOneObject() {
    assertThat(listOf(Map.of("sp_title", "a"))).isEmpty();
  }

  @Test
  void answersNoElementForALeafField() {
    assertThat(listOf("text")).isEmpty();
    assertThat(listOf(42)).isEmpty();
  }

  @Test
  void answersNoElementForAMissingField() {
    Repository repository = new Repository(Map.of("1", Map.of("id", "1", "content", Map.of())));
    Resolver page = ResolverPath.createRoot(path -> repository.resolver(path, "1"));

    assertThat(page.resolveList("sp_items")).isEmpty();
  }

  @Test
  void answersNoElementForAnObjectFieldWithoutAnyKey() {
    assertThat(listOf(Map.of())).isEmpty();
  }

  @Test
  void skipsAnElementThatIsNoObject() {
    List<Resolver> items = listOf(List.of("2", Map.of("sp_title", "b")));

    assertThat(items).hasSize(1);
    assertThat(items.getFirst().value("sp_title").asString("")).isEqualTo("b");
  }

  @Test
  void keepsTheListPositionOfAnElementAfterASkippedOne() {
    List<Resolver> withSkip = listOf(List.of("x", Map.of("sp_title", "b")));
    List<Resolver> without = listOf(List.of(Map.of("sp_title", "a"), Map.of("sp_title", "b")));

    assertThat(withSkip.getFirst().nodeKey()).isEqualTo(without.get(1).nodeKey());
  }
}
