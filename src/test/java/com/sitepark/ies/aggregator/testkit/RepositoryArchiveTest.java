package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitepark.ies.aggregator.resolver.EntityResolver;
import com.sitepark.ies.aggregator.resolver.ResolverPath;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * An entry declares {@code "archive": true} to be archived. Production decides the archive state
 * for articles only, so a group or a medium answers {@code false} whatever it declares.
 */
class RepositoryArchiveTest {

  private static boolean isArchive(Map<String, Object> entry) {
    Repository repository = new Repository(Map.of("1", entry));
    EntityResolver resolver =
        (EntityResolver) ResolverPath.createRoot(path -> repository.resolver(path, "1"));
    return resolver.entity().isArchive();
  }

  @Test
  void answersTrueForADeclaredArchive() {
    assertThat(isArchive(Map.of("id", "1", "archive", true))).isTrue();
  }

  @Test
  void answersFalseWithoutTheFlag() {
    assertThat(isArchive(Map.of("id", "1"))).isFalse();
  }

  @Test
  void answersFalseForArchiveFalse() {
    assertThat(isArchive(Map.of("id", "1", "archive", false))).isFalse();
  }

  @Test
  void answersFalseForAGroupThatDeclaresIt() {
    assertThat(isArchive(Map.of("id", "1", "archive", true, "group", Map.of()))).isFalse();
  }

  @Test
  void answersFalseForAMediumThatDeclaresIt() {
    assertThat(isArchive(Map.of("id", "1", "archive", true, "media", Map.of()))).isFalse();
  }
}
