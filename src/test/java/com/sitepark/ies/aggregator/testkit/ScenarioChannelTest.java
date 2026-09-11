package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitepark.ies.aggregator.port.Channel;
import com.sitepark.ies.aggregator.value.uri.UriTarget;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the testkit channel answers a target only where the publication would.
 *
 * <p>The channel used to resolve every target alike. That generosity hid a published bug: a
 * standalone medium has no page URL, and asking for one has to come back empty — otherwise no
 * scenario can show that an assembler asked the wrong question.
 */
class ScenarioChannelTest {

  private static Channel channel(Map<String, Object> entries) {
    return new ScenarioChannelProvider(new Repository(entries), null).current();
  }

  @Test
  void answersNoPageUrlForAStandaloneMedium() {
    Channel channel = channel(Map.of("1000", Map.of("id", 1000, "media", Map.of("id", 4712))));

    assertThat(channel.resolveUri(UriTarget.ofObject(1000)))
        .as("a standalone medium is published under its binary and has no page of its own")
        .isEmpty();
  }

  @Test
  void answersThePageUrlOfAnOrdinaryResource() {
    Channel channel = channel(Map.of("1000", Map.of("id", 1000)));

    assertThat(channel.resolveUri(UriTarget.ofObject(1000)).map(Object::toString))
        .as("an ordinary resource resolves to the page URL of its object id")
        .contains("https://example.com/object/1000");
  }

  @Test
  void answersThePageUrlOfAResourceThatMerelyHoldsAnUpload() {
    Channel channel =
        channel(
            Map.of(
                "1000",
                Map.of(
                    "id", 1000, "content", Map.of("sp_imageUpload", Map.of("media", Map.of())))));

    assertThat(channel.resolveUri(UriTarget.ofObject(1000)))
        .as("an upload below a field does not turn its article into a medium")
        .isPresent();
  }

  @Test
  void answersTheBinaryUrlOfAStandaloneMedium() {
    Channel channel = channel(Map.of("1000", Map.of("id", 1000, "media", Map.of("id", 4712))));

    assertThat(channel.resolveUri(UriTarget.ofMedia(1000, 4712)).map(Object::toString))
        .as("only the binary target resolves for a medium")
        .contains("https://example.com/media/1000/4712");
  }

  @Test
  void answersWithAnAbsoluteUrl() {
    Channel channel = channel(Map.of("1000", Map.of("id", 1000)));

    assertThat(channel.resolveUri(UriTarget.ofObject(1000)).map(uri -> uri.scheme()))
        .as(
            "the production channel hands out baseUrl plus path; reducing it is the aggregator's"
                + " job")
        .contains("https");
  }
}
