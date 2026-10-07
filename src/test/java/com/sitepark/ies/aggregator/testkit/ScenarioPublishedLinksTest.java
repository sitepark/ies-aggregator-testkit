package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * The harness records what the publisher would store, and drops what the production adapter drops:
 * a scenario must not show a report that never reaches the link checker.
 */
class ScenarioPublishedLinksTest {

  @Test
  void recordsReportsWithAndWithoutScopeInOrder() {
    ScenarioPublishedLinks links = new ScenarioPublishedLinks();
    FieldResolver scope = FieldResolver.empty();

    links.external(scope, "https://example.org/a");
    links.external("http://example.org/b");

    assertThat(links.reported())
        .containsExactly(
            new PublishedLink(scope, "https://example.org/a"),
            new PublishedLink(null, "http://example.org/b"));
  }

  @Test
  void dropsWhatIsNoAbsoluteUrl() {
    ScenarioPublishedLinks links = new ScenarioPublishedLinks();

    links.external("/relative/path");
    links.external("javascript:void(0)");
    links.external(FieldResolver.empty(), "https://exa mple.org");

    assertThat(links.reported()).isEmpty();
  }
}
