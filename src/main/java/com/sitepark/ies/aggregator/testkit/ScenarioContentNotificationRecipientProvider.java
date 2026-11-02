package com.sitepark.ies.aggregator.testkit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitepark.ies.aggregator.port.ContentNotificationRecipientProvider;
import com.sitepark.ies.aggregator.value.notification.ContentNotificationRecipients;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Answers the recipients of a content notification from the scenario's {@code userGroups} block, a
 * map {@code anchor -> {"qualifiedId": "...", "members": [{"email": ["a@x", ...]}, ...]}}.
 *
 * <p>Only a group the block names is answered; an unknown anchor, and a scenario without the block,
 * answers empty - as production does when the user repository has no group of that anchor. The
 * rules are production's: the inbox recipient is the group's qualified id, the addresses are those
 * of the direct members only, each member contributing its <em>first</em> address, trimmed, and
 * none when that is blank or the member has no address (a later address is never a fallback). The
 * order of the members is kept and nothing is de-duplicated - the same address of two members stays
 * twice, because production does not remove it either.
 */
final class ScenarioContentNotificationRecipientProvider
    implements ContentNotificationRecipientProvider {

  private final Map<String, Group> groups;

  ScenarioContentNotificationRecipientProvider(ObjectMapper mapper, @Nullable Object rawGroups) {
    this.groups =
        rawGroups == null
            ? Map.of()
            : mapper.convertValue(rawGroups, new TypeReference<Map<String, Group>>() {});
  }

  @Override
  public Optional<ContentNotificationRecipients> recipients(String reference) {
    Group group = this.groups.get(reference);
    if (group == null) {
      return Optional.empty();
    }
    if (group.qualifiedId() == null || group.qualifiedId().isBlank()) {
      throw new IllegalArgumentException(
          "user group '" + reference + "' must declare a non-blank 'qualifiedId'");
    }
    List<String> addresses = new ArrayList<>();
    for (Member member : group.members() == null ? List.<Member>of() : group.members()) {
      List<String> emails = member.email();
      if (emails == null || emails.isEmpty() || emails.get(0) == null) {
        continue;
      }
      String first = emails.get(0).trim();
      if (!first.isEmpty()) {
        addresses.add(first);
      }
    }
    return Optional.of(
        new ContentNotificationRecipients(group.qualifiedId(), List.copyOf(addresses)));
  }

  private record Group(@Nullable String qualifiedId, @Nullable List<Member> members) {}

  private record Member(@Nullable List<String> email) {}
}
