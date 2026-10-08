package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitepark.ies.aggregator.resolver.EntityKind;
import com.sitepark.ies.aggregator.resolver.Resolver;
import com.sitepark.ies.aggregator.resolver.ResolverPath;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * An entry's kind comes from what the entry is - a media block, a group block, or the master data
 * that names a resource article - never from a field an editor could fill.
 */
class RepositoryEntityKindTest {

  private static EntityKind kindOf(Map<String, Object> entry) {
    Repository repository = new Repository(Map.of("1", entry));
    Resolver resolver = ResolverPath.createRoot(path -> repository.resolver(path, "1"));
    return ((RepositoryResolver) resolver).entity().kind();
  }

  @Test
  void anOrdinaryEntryIsAPage() {
    assertThat(kindOf(Map.of("id", "1"))).isEqualTo(EntityKind.PAGE);
  }

  @Test
  void anEntryWithItsOwnMediaBlockIsAMedium() {
    assertThat(kindOf(Map.of("id", "1", "media", Map.of("mime", "image/jpeg"))))
        .isEqualTo(EntityKind.MEDIA);
  }

  @Test
  void anEntryWithAGroupBlockIsAGroup() {
    assertThat(kindOf(Map.of("id", "1", "group", Map.of()))).isEqualTo(EntityKind.GROUP);
  }

  @Test
  void onlyTheMasterDataNamesAResourceArticle() {
    assertThat(kindOf(Map.of("id", "1", "entityKind", "resource"))).isEqualTo(EntityKind.RESOURCE);
    assertThat(kindOf(Map.of("id", "1", "content", Map.of("entityKind", "resource"))))
        .as("a field of that name is content, not the kind")
        .isEqualTo(EntityKind.PAGE);
  }
}
