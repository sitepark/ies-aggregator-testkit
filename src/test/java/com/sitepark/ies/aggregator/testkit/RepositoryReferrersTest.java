package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitepark.ies.aggregator.resolver.EntityResolver;
import com.sitepark.ies.aggregator.resolver.Resolver;
import com.sitepark.ies.aggregator.resolver.ResolverPath;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The referrers of an entry are derived from the repository: the entries whose link field names it,
 * as production answers from its link index - published or not, each once.
 */
class RepositoryReferrersTest {

  private static final String FIELD = "sp_parent_navigation.link";

  private static Map<String, Object> page(String id, Map<String, Object> content) {
    return Map.of("id", id, "content", content);
  }

  /** A repository in the given order, the target {@code 1} first. */
  private static Repository repository(Object... idsAndEntries) {
    Map<String, Object> entries = new LinkedHashMap<>();
    entries.put("1", page("1", Map.of()));
    for (int i = 0; i < idsAndEntries.length; i += 2) {
      entries.put((String) idsAndEntries[i], idsAndEntries[i + 1]);
    }
    return new Repository(entries);
  }

  @Test
  void findsADirectLink() {
    Repository repository = repository("2", page("2", Map.of("sp_parent_navigation", "1")));

    assertThat(repository.referrersOf("1", FIELD)).containsExactly("2");
  }

  @Test
  void findsALinkInsideAnIterateList() {
    Repository repository =
        repository(
            "2",
            page(
                "2",
                Map.of(
                    "sp_parent_navigation_iterate",
                    List.of(
                        Map.of("sp_parent_navigation", "9"),
                        Map.of("sp_parent_navigation", Map.of("link", "1"))))));

    assertThat(repository.referrersOf("1", FIELD)).containsExactly("2");
  }

  @Test
  void findsTheLinkWrapperForm() {
    Repository repository =
        repository("2", page("2", Map.of("sp_parent_navigation", Map.of("link", "1"))));

    assertThat(repository.referrersOf("1", FIELD)).containsExactly("2");
  }

  @Test
  void findsAListOfLinks() {
    Repository repository =
        repository(
            "2",
            page("2", Map.of("sp_parent_navigation", List.of("9", "1"))),
            "3",
            page("3", Map.of("sp_parent_navigation", Map.of("link", List.of("1", "9")))));

    assertThat(repository.referrersOf("1", FIELD)).containsExactly("2", "3");
  }

  @Test
  void listsAnEntryThatLinksTwiceOnce() {
    Repository repository =
        repository(
            "2",
            page(
                "2",
                Map.of(
                    "sp_parent_navigation",
                    "1",
                    "sp_other_iterate",
                    List.of(Map.of("sp_parent_navigation", "1")))));

    assertThat(repository.referrersOf("1", FIELD)).containsExactly("2");
  }

  @Test
  void includesAnUnpublishedReferrer() {
    Repository repository =
        repository(
            "2",
            Map.of("id", "2", "published", false, "content", Map.of("sp_parent_navigation", "1")));

    assertThat(repository.referrersOf("1", FIELD))
        .as("production does not filter the referrers by publication")
        .containsExactly("2");
  }

  @Test
  void doesNotMatchAnUnrelatedField() {
    Repository repository = repository("2", page("2", Map.of("sp_related", "1")));

    assertThat(repository.referrersOf("1", FIELD)).isEmpty();
  }

  @Test
  void ignoresTheLinkType() {
    Repository repository = repository("2", page("2", Map.of("sp_parent_navigation", "1")));

    assertThat(repository.referrersOf("1", "sp_parent_navigation.other")).containsExactly("2");
  }

  @Test
  void leavesOutAGroup() {
    Repository repository =
        repository(
            "2",
            Map.of("id", "2", "group", Map.of(), "content", Map.of("sp_parent_navigation", "1")));

    assertThat(repository.referrersOf("1", FIELD)).isEmpty();
  }

  @Test
  void answersTheReferrersInRepositoryOrder() {
    Repository repository =
        repository(
            "5",
            page("5", Map.of("sp_parent_navigation", "1")),
            "3",
            page("3", Map.of("sp_parent_navigation", "1")));

    assertThat(repository.referrersOf("1", FIELD)).containsExactly("5", "3");
  }

  @Test
  void answersNothingWithoutAReferrer() {
    assertThat(repository().referrersOf("1", FIELD)).isEmpty();
  }

  @Test
  void theResolverEntersEachReferrerAsARoot() {
    Repository repository =
        repository(
            "2",
            page("2", Map.of("sp_parent_navigation", "1")),
            "3",
            page("3", Map.of("sp_parent_navigation", "1")));
    Resolver target = ResolverPath.createRoot(path -> repository.resolver(path, "1"));

    List<EntityResolver> referrers = ((EntityResolver) target).resolveReferrers(FIELD);

    assertThat(referrers.stream().map(referrer -> referrer.entity().id())).containsExactly(2, 3);
  }

  @Test
  void theResolverAnswersNothingWithoutReferrers() {
    Repository repository = repository();
    Resolver target = ResolverPath.createRoot(path -> repository.resolver(path, "1"));

    assertThat(((EntityResolver) target).resolveReferrers(FIELD)).isEmpty();
  }
}
