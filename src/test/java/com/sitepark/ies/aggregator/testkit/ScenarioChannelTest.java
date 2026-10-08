package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitepark.ies.aggregator.port.Channel;
import com.sitepark.ies.aggregator.port.UrlLookup;
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

  private static final String CHANNEL_SCENARIO = "testkit/channel.json";

  /** Any layout will do: these tests never aggregate, they only ask the channel. */
  private static final ScenarioLayout LAYOUT =
      new ScenarioLayout("area", "root", "container", "container");

  private static Channel channel(Map<String, Object> entries) {
    return channel(entries, ScenarioChannelConfig.EMPTY);
  }

  private static Channel channel(Map<String, Object> entries, ScenarioChannelConfig config) {
    return new ScenarioChannelProvider(new Repository(entries), null, config).current();
  }

  @Test
  void answersTheMediaUrlForAStandaloneMedium() {
    Channel channel = channel(Map.of("1000", Map.of("id", 1000, "media", Map.of("id", 4712))));

    assertThat(channel.resolveUri(UriTarget.ofObject(1000)).map(Object::toString))
        .as(
            "a standalone medium has no page of its own, so an object target answers with the"
                + " download URL of the medium it publishes - the caller must not have to know"
                + " what kind of object it is pointing at")
        .contains("/media/1000/4712");
  }

  @Test
  void answersNothingForAStandaloneMediumWithoutAnAssetId() {
    Channel channel = channel(Map.of("1000", Map.of("id", 1000, "media", Map.of())));

    assertThat(channel.resolveUri(UriTarget.ofObject(1000)))
        .as("without an asset id there is no binary to name, and no page URL is invented for it")
        .isEmpty();
  }

  @Test
  void answersThePageUrlOfAnOrdinaryResource() {
    Channel channel = channel(Map.of("1000", Map.of("id", 1000)));

    assertThat(channel.resolveUri(UriTarget.ofObject(1000)).map(Object::toString))
        .as("an ordinary resource resolves to the page URL of its object id")
        .contains("/object/1000");
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
        .contains("/media/1000/4712");
  }

  @Test
  void declaresNoNatureUnlessTheScenarioNamesOne() {
    Channel channel = channel(Map.of("1000", Map.of("id", 1000)));

    assertThat(channel.nature())
        .as("a scenario that names no nature gets a channel that declares none, as production does")
        .isEmpty();
  }

  @Test
  void answersTheNatureTheScenarioNames() {
    Channel channel =
        channel(
            Map.of("1000", Map.of("id", 1000)),
            ScenarioChannelConfig.of("intranet", Map.of(), null, null));

    assertThat(channel.nature())
        .as("a rule keyed to an internal web has to be shown the scenario that declares one")
        .contains("intranet");
  }

  @Test
  void answersOnlyTheAttributesTheScenarioNames() {
    Channel channel =
        channel(
            Map.of("1000", Map.of("id", 1000)),
            ScenarioChannelConfig.of(null, Map.of("sp_vv_mode", "intern"), null, null));

    assertThat(channel.attribute("sp_vv_mode"))
        .as("the attribute the scenario sets is answered verbatim")
        .contains("intern");
    assertThat(channel.attribute("sp_vv_isIntern"))
        .as("an attribute nobody set stays unset - it is not the same as set to false")
        .isEmpty();
  }

  @Test
  void readsNatureAndAttributesFromTheScenarioFile() {
    Channel channel = ScenarioContext.load(CHANNEL_SCENARIO, LAYOUT).channel();

    assertThat(channel.nature())
        .as("the nature the scenario file declares reaches the channel")
        .contains("intranet");
    assertThat(channel.attribute("sp_vv_mode"))
        .as("so does an attribute below the channel block")
        .contains("intern");
  }

  @Test
  void answersTheUrlLookupOnlyWhereTheScenarioNamesOne() {
    Map<String, Object> repository = Map.of("1000", Map.of("id", 1000));

    assertThat(channel(repository, ScenarioChannelConfig.EMPTY).urlLookup())
        .as("a nature without urlLookup links no object it does not publish")
        .isEmpty();
    assertThat(
            channel(repository, ScenarioChannelConfig.of(null, Map.of(), UrlLookup.STRICT, null))
                .urlLookup())
        .contains(UrlLookup.STRICT);
  }

  @Test
  void answersAPrimaryChannelOnlyWhereTheScenarioDeclaresOne() {
    Repository repository = new Repository(Map.of("1000", Map.of("id", 1000)));

    assertThat(
            new ScenarioChannelProvider(repository, null, ScenarioChannelConfig.EMPTY)
                .primary(1000))
        .as("production answers empty for an object no pool assigns a primary channel")
        .isEmpty();
    assertThat(
            new ScenarioChannelProvider(
                    repository, null, ScenarioChannelConfig.of(null, Map.of(), null, true))
                .primary(1000))
        .isPresent();
  }

  @Test
  void answersAPathInTheCurrentChannelAndTheFullUrlInAnother() {
    Repository repository = new Repository(Map.of("1000", Map.of("id", 1000)));
    ScenarioChannelProvider provider =
        new ScenarioChannelProvider(
            repository, null, ScenarioChannelConfig.of(null, Map.of(), null, true));

    assertThat(provider.current().resolveUri(UriTarget.ofObject(1000)).map(Object::toString))
        .as("a page of the current site links to the target by its path")
        .contains("/object/1000");
    assertThat(
            provider
                .primary(1000)
                .flatMap(channel -> channel.resolveUri(UriTarget.ofObject(1000)))
                .map(Object::toString))
        .as("another channel keeps its host, or the link would point into the current site")
        .contains("https://example.com/object/1000");
  }
}
