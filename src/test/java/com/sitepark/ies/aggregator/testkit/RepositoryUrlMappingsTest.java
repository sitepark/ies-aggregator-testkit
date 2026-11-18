package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.sitepark.ies.aggregator.resolver.GroupDescriptor;
import com.sitepark.ies.aggregator.resolver.Resolver;
import com.sitepark.ies.aggregator.resolver.ResolverPath;
import com.sitepark.ies.aggregator.value.SiteUrlMapping;
import com.sitepark.ies.aggregator.value.UrlMappingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** A group answers URL mappings only where the scenario names them. */
class RepositoryUrlMappingsTest {

  private static GroupDescriptor descriptorOf(Map<String, Object> entry) {
    Repository repository = new Repository(Map.of("1", entry));
    Resolver resolver = ResolverPath.createRoot(path -> repository.resolver(path, "1"));
    return ((RepositoryResolver) resolver).entity();
  }

  private static Map<String, Object> groupWith(Object urlMappings) {
    return Map.of("id", "1", "group", Map.of("urlMappings", urlMappings));
  }

  @Test
  void aGroupWithoutTheBlockHasNoMappings() {
    assertThat(descriptorOf(Map.of("id", "1", "group", Map.of())).urlMappings()).isEmpty();
  }

  @Test
  void anEntryWithoutAGroupBlockHasNoMappings() {
    assertThat(descriptorOf(Map.of("id", "1")).urlMappings()).isEmpty();
  }

  @Test
  void theBlockIsAnsweredInItsOrder() {
    GroupDescriptor group =
        descriptorOf(
            groupWith(
                List.of(
                    Map.of("path", "/b", "ownerId", "7", "mode", "FORWARD", "locale", "de_DE"),
                    Map.of("path", "/a", "ownerId", "8", "mode", "REDIRECT_PERMANENT"))));

    assertThat(group.urlMappings())
        .containsExactly(
            new SiteUrlMapping("/b", "7", UrlMappingMode.FORWARD, Locale.GERMANY),
            new SiteUrlMapping("/a", "8", UrlMappingMode.REDIRECT_PERMANENT, null));
  }

  @Test
  void aMappingWithoutPathIsRejected() {
    GroupDescriptor group =
        descriptorOf(groupWith(List.of(Map.of("ownerId", "7", "mode", "FORWARD"))));

    assertThatIllegalArgumentException().isThrownBy(group::urlMappings);
  }

  @Test
  void aMappingWithoutOwnerIsRejected() {
    GroupDescriptor group =
        descriptorOf(groupWith(List.of(Map.of("path", "/a", "mode", "FORWARD"))));

    assertThatIllegalArgumentException().isThrownBy(group::urlMappings);
  }

  @Test
  void aMappingWithoutModeIsRejectedNotDefaulted() {
    GroupDescriptor group = descriptorOf(groupWith(List.of(Map.of("path", "/a", "ownerId", "7"))));

    assertThatIllegalArgumentException().isThrownBy(group::urlMappings);
  }

  @Test
  void anUnknownModeIsRejected() {
    GroupDescriptor group =
        descriptorOf(groupWith(List.of(Map.of("path", "/a", "ownerId", "7", "mode", "TELEPORT"))));

    assertThatIllegalArgumentException().isThrownBy(group::urlMappings);
  }

  @Test
  void aPathTwiceIsRejectedBecauseTheStoreKeepsOnePerPath() {
    GroupDescriptor group =
        descriptorOf(
            groupWith(
                List.of(
                    Map.of("path", "/a", "ownerId", "7", "mode", "FORWARD"),
                    Map.of("path", "/a", "ownerId", "8", "mode", "FORWARD"))));

    assertThatIllegalArgumentException().isThrownBy(group::urlMappings);
  }

  @Test
  void aBlockThatIsNoListIsRejected() {
    GroupDescriptor group = descriptorOf(groupWith("nope"));

    assertThatIllegalArgumentException().isThrownBy(group::urlMappings);
  }

  @Test
  void anEntryThatIsNoObjectIsRejected() {
    GroupDescriptor group = descriptorOf(groupWith(List.of("nope")));

    assertThatIllegalArgumentException().isThrownBy(group::urlMappings);
  }
}
