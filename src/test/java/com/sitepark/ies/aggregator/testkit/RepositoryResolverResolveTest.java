package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sitepark.ies.aggregator.resolver.Resolver;
import com.sitepark.ies.aggregator.resolver.ResolverPath;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@code resolve} navigates into objects only. Production holds every text, number, date, url and
 * link field as a {@code SimpleInformationVO}, which {@code ResolverContext.forInformation} refuses
 * with {@code UnsupportedOperationException}; a link is followed with {@code resolveLink}.
 */
class RepositoryResolverResolveTest {

  private static final Map<String, Object> TARGET = Map.of("id", "2", "objectType", "news");

  private static Resolver page(Object field) {
    Repository repository =
        new Repository(
            Map.of("1", Map.of("id", "1", "content", Map.of("sp_field", field)), "2", TARGET));
    return ResolverPath.createRoot(path -> repository.resolver(path, "1"));
  }

  @Test
  void refusesALinkWrapper() {
    Resolver page = page(Map.of("link", "2"));

    assertThatThrownBy(() -> page.resolve("sp_field"))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("SimpleInformationVO not resolvable");
  }

  @Test
  void refusesABareIdReference() {
    Resolver page = page("2");

    assertThatThrownBy(() -> page.resolve("sp_field"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void refusesAText() {
    Resolver page = page("hello");

    assertThatThrownBy(() -> page.resolve("sp_field"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void refusesANumber() {
    Resolver page = page(42);

    assertThatThrownBy(() -> page.resolve("sp_field"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void stillFollowsTheLinkWithResolveLink() {
    assertThat(page(Map.of("link", "2")).resolveLink("sp_field").isEmpty()).isFalse();
  }

  @Test
  void navigatesIntoAnObject() {
    Resolver page = page(Map.of("sp_title", "inner"));

    assertThat(page.resolve("sp_field").value("sp_title").asString()).isEqualTo("inner");
  }

  @Test
  void answersEmptyForAnAbsentField() {
    assertThat(page(Map.of("sp_title", "x")).resolve("sp_missing").isEmpty()).isTrue();
  }
}
