package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitepark.ies.aggregator.resolver.EntityResolver;
import com.sitepark.ies.aggregator.resolver.GroupResolver;
import com.sitepark.ies.aggregator.resolver.Resolver;
import com.sitepark.ies.aggregator.resolver.ResolverPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * A link field is followed only when it names an entry, as {@code
 * BaseInformationVOResolver.resolveLink} follows one only when it carries a link id whose article
 * loads. Following anything else would make the harness more generous than production - a port
 * reading the link from the wrong level got an entity with id {@code 0} here and none in production.
 */
class RepositoryResolverLinkTest {

  private static final Map<String, Object> TARGET = Map.of("id", "2", "objectType", "news");

  private static EntityResolver follow(Object field) {
    Repository repository =
        new Repository(
            Map.of("1", Map.of("id", "1", "content", Map.of("sp_link", field)), "2", TARGET));
    Resolver page = ResolverPath.createRoot(path -> repository.resolver(path, "1"));
    return page.resolveLink("sp_link");
  }

  @Test
  void followsAnIdReference() {
    assertThat(follow("2").isEmpty()).isFalse();
  }

  @Test
  void followsALinkWrapper() {
    assertThat(follow(Map.of("link", "2")).isEmpty()).isFalse();
  }

  @Test
  void followsAnIdReferenceWithOverrides() {
    assertThat(follow(Map.of("$ref", "2", "headline", "overridden")).isEmpty()).isFalse();
  }

  @Test
  void doesNotFollowAnInlineObject() {
    assertThat(follow(Map.of("sp_linkType", "external", "sp_url", "https://example.com")).isEmpty())
        .as("a sub-information without a link is no link target")
        .isTrue();
  }

  @Test
  void doesNotFollowAnUnknownId() {
    assertThat(follow("4711").isEmpty()).as("an article that does not load").isTrue();
  }

  @Test
  void doesNotFollowALinkWrapperAroundAnInlineObject() {
    assertThat(follow(Map.of("link", Map.of("sp_url", "https://example.com"))).isEmpty()).isTrue();
  }

  private static List<EntityResolver> followAll(Object field) {
    Repository repository =
        new Repository(
            Map.of(
                "1",
                Map.of("id", "1", "content", Map.of("sp_categoryRoots", field)),
                "2",
                TARGET,
                "3",
                Map.of("id", "3", "objectType", "news")));
    Resolver page = ResolverPath.createRoot(path -> repository.resolver(path, "1"));
    return page.resolveLinks("sp_categoryRoots");
  }

  private static List<Integer> ids(List<EntityResolver> linked) {
    return linked.stream().map(entity -> entity.entity().id()).toList();
  }

  @Test
  void followsEveryLinkOfALinkList() {
    assertThat(ids(followAll(Map.of("link", List.of("3", "2")))))
        .as("every link, in stored order")
        .containsExactly(3, 2);
  }

  @Test
  void followsEveryIdOfABareList() {
    assertThat(ids(followAll(List.of("2", "3")))).containsExactly(2, 3);
  }

  @Test
  void followsASingleLinkAsAListOfOne() {
    assertThat(ids(followAll(Map.of("link", "2")))).containsExactly(2);
  }

  @Test
  void leavesOutWhatNamesNoEntry() {
    assertThat(ids(followAll(Map.of("link", List.of("4711", "2", Map.of("sp_url", "x"))))))
        .as("an unknown id and an inline object are no link targets, as in production")
        .containsExactly(2);
  }

  @Test
  void answersNothingForAnAbsentField() {
    Repository repository = new Repository(Map.of("1", Map.of("id", "1", "content", Map.of())));
    Resolver page = ResolverPath.createRoot(path -> repository.resolver(path, "1"));
    assertThat(page.resolveLinks("sp_categoryRoots")).isEmpty();
  }

  private static Resolver pageLinking(Object field) {
    Repository repository =
        new Repository(
            Map.of(
                "1",
                Map.of("id", "1", "content", Map.of("sp_galleryGroup", field)),
                "2",
                TARGET,
                "7",
                Map.of("id", "7", "group", Map.of("entities", List.of("2")))));
    return ResolverPath.createRoot(path -> repository.resolver(path, "1"));
  }

  @Test
  void resolveLinkDoesNotFollowAGroup() {
    assertThat(pageLinking("7").resolveLink("sp_galleryGroup").isEmpty())
        .as("production loads the target as an article and answers none for a pool")
        .isTrue();
  }

  @Test
  void resolveLinksLeavesOutAGroup() {
    assertThat(pageLinking(List.of("7", "2")).resolveLinks("sp_galleryGroup"))
        .as("only the entity, not the group")
        .hasSize(1);
  }

  @Test
  void resolveGroupLinkFollowsAGroup() {
    GroupResolver group = pageLinking(Map.of("link", "7")).resolveGroupLink("sp_galleryGroup");
    assertThat(group.isEmpty()).as("the linked group").isFalse();
    assertThat(group.entities()).as("its members").hasSize(1);
  }

  @Test
  void resolveGroupLinkDoesNotFollowAnEntity() {
    assertThat(pageLinking("2").resolveGroupLink("sp_galleryGroup").isEmpty())
        .as("an entity is no group link target")
        .isTrue();
  }

  @Test
  void resolveGroupLinkAnswersNothingForAnUnknownId() {
    assertThat(pageLinking("4711").resolveGroupLink("sp_galleryGroup").isEmpty()).isTrue();
  }
}
