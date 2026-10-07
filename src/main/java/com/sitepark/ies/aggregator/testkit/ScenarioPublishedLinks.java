package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.port.PublishedLinks;
import com.sitepark.ies.aggregator.resolver.Resolver;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * {@link PublishedLinks} of a scenario: records every report, so a test can ask what the publisher
 * would have stored.
 *
 * <p>No more generous than production: a URL the production adapter drops - one that does not read
 * as an absolute URL - is not recorded here either.
 */
final class ScenarioPublishedLinks implements PublishedLinks {

  private final List<PublishedLink> reported = new ArrayList<>();

  @Override
  public void external(Resolver scope, String url) {
    this.record(scope, url);
  }

  @Override
  public void external(String url) {
    this.record(null, url);
  }

  List<PublishedLink> reported() {
    return List.copyOf(this.reported);
  }

  private void record(@Nullable Resolver scope, String url) {
    if (isUrl(url)) {
      this.reported.add(new PublishedLink(scope, url));
    }
  }

  private static boolean isUrl(String url) {
    try {
      URI.create(url).toURL();
      return true;
    } catch (IllegalArgumentException | MalformedURLException e) {
      return false;
    }
  }
}
