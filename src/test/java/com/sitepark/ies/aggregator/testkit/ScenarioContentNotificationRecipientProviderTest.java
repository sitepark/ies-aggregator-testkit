package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitepark.ies.aggregator.value.notification.ContentNotificationRecipients;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** The recipients of a content notification follow the rules production applies to a user group. */
class ScenarioContentNotificationRecipientProviderTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static ScenarioContentNotificationRecipientProvider provider(Object groups) {
    return new ScenarioContentNotificationRecipientProvider(MAPPER, groups);
  }

  private static Map<String, Object> group(String qualifiedId, Object... members) {
    return Map.of("qualifiedId", qualifiedId, "members", java.util.List.of(members));
  }

  private static Map<String, Object> member(String... emails) {
    return Map.of("email", java.util.List.of(emails));
  }

  @Test
  void answersTheQualifiedIdAndTheAddressesOfTheMembers() {
    var provider =
        provider(Map.of("editors", group("user.group.7", member("a@x.de"), member("b@x.de"))));

    assertThat(provider.recipients("editors"))
        .contains(
            new ContentNotificationRecipients(
                "user.group.7", java.util.List.of("a@x.de", "b@x.de")));
  }

  @Test
  void answersEmptyForAnAnchorTheBlockDoesNotName() {
    var provider = provider(Map.of("editors", group("user.group.7")));

    assertThat(provider.recipients("other")).isEmpty();
  }

  @Test
  void answersEmptyWithoutTheBlock() {
    assertThat(provider(null).recipients("editors")).isEmpty();
  }

  @Test
  void takesOnlyTheFirstAddressOfAMember() {
    var provider = provider(Map.of("g", group("q", member("first@x.de", "second@x.de"))));

    assertThat(provider.recipients("g").orElseThrow().emailAddresses())
        .containsExactly("first@x.de");
  }

  @Test
  void trimsTheAddress() {
    var provider = provider(Map.of("g", group("q", member("  a@x.de \t"))));

    assertThat(provider.recipients("g").orElseThrow().emailAddresses()).containsExactly("a@x.de");
  }

  @Test
  void skipsABlankFirstAddressWithoutFallingBackToTheNext() {
    var provider =
        provider(Map.of("g", group("q", member("   ", "second@x.de"), member("b@x.de"))));

    assertThat(provider.recipients("g").orElseThrow().emailAddresses()).containsExactly("b@x.de");
  }

  @Test
  void skipsAMemberWithoutAnyAddress() {
    var provider =
        provider(
            Map.of(
                "g",
                group(
                    "q",
                    Map.of(),
                    member(),
                    Map.of("email", java.util.List.of()),
                    member("b@x.de"))));

    assertThat(provider.recipients("g").orElseThrow().emailAddresses()).containsExactly("b@x.de");
  }

  @Test
  void keepsADuplicateAddressAsProductionDoes() {
    var provider = provider(Map.of("g", group("q", member("a@x.de"), member("a@x.de"))));

    assertThat(provider.recipients("g").orElseThrow().emailAddresses())
        .containsExactly("a@x.de", "a@x.de");
  }

  @Test
  void answersAGroupWithoutMembersWithNoAddresses() {
    var provider = provider(Map.of("g", Map.of("qualifiedId", "q")));

    Optional<ContentNotificationRecipients> recipients = provider.recipients("g");

    assertThat(recipients).isPresent();
    assertThat(recipients.orElseThrow().inboxRecipient()).isEqualTo("q");
    assertThat(recipients.orElseThrow().emailAddresses()).isEmpty();
  }

  @Test
  void failsForAGroupWithoutAQualifiedId() {
    var provider = provider(Map.of("g", Map.of("members", java.util.List.of())));

    assertThatThrownBy(() -> provider.recipients("g")).isInstanceOf(IllegalArgumentException.class);
  }
}
