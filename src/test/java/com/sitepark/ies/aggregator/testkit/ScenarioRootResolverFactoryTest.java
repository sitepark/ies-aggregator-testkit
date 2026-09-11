package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sitepark.ies.aggregator.resolver.EntityResolver;
import com.sitepark.ies.aggregator.resolver.GroupResolver;
import com.sitepark.ies.aggregator.resolver.Resolver;
import com.sitepark.ies.aggregator.resolver.RootResolverFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Verifies how the testkit addresses objects that are not linked from the section source: by id and
 * by anchor, as an entity and as a group.
 *
 * <p>The fixture holds two entities ({@code 1000}/{@code start-page} and {@code 1330}/{@code
 * target-page}) below the site-root group {@code 5000}/{@code site-root}, which in turn contains the
 * microsite group {@code 5100}/{@code microsite-root}.
 */
class ScenarioRootResolverFactoryTest {

  private static final String SCENARIO = "testkit/root-resolver.json";

  /** Any layout will do: this test never aggregates, it only addresses objects of the scenario. */
  private static final ScenarioLayout LAYOUT =
      new ScenarioLayout("area", "root", "container", "container");

  private static RootResolverFactory factory() {
    return ScenarioContext.load(SCENARIO, LAYOUT).rootResolverFactory();
  }

  @Test
  void resolvesAnEntityByItsId() {
    EntityResolver entity = factory().createByEntityId(1330);

    assertThat(entity.value("sp_headline").asString(""))
        .as("an entity addressed by id reads the fields of that entry")
        .isEqualTo("Target page");
  }

  @Test
  void resolvesAnEntityByItsAnchor() {
    EntityResolver entity = factory().createByEntityAnchor("target-page");

    assertThat(entity.entity().id())
        .as("an anchor addresses the same entity as its id")
        .isEqualTo(1330);
  }

  @Test
  void resolvesAGroupByItsAnchorIncludingItsGroupData() {
    GroupResolver group = factory().createByGroupAnchor("site-root");

    assertThat(group.entity().id()).as("the group addressed by the anchor").isEqualTo(5000);
    assertThat(group.entity().lang()).as("the language configured on the group").isEqualTo("de");
    assertThat(group.entity().isRootSite()).as("the group is marked as a site root").isTrue();
    assertThat(group.entity().isMicrositeRootSite())
        .as("the site root is not a microsite root")
        .isFalse();
  }

  @Test
  void resolvesAGroupsChildrenAsSubGroupsFollowedByEntities() {
    GroupResolver group = factory().createByGroupId(5000);

    assertThat(group.children().stream().map(child -> child.entity().id()))
        .as("the group lists its sub-groups before its entities")
        .containsExactly(5100, 1000, 1330);
  }

  @Test
  void aGroupWithoutMemberListsHasNoChildren() {
    GroupResolver group = factory().createByGroupAnchor("microsite-root");

    assertThat(group.children())
        .as("a group block that lists neither sub-groups nor entities has no children")
        .isEmpty();
  }

  @Test
  void anEntityLookupNeverReturnsAGroup() {
    EntityResolver entity = factory().createByEntityAnchor("site-root");

    assertThat(entity.isEmpty()).as("the anchor of a group does not address an entity").isTrue();
  }

  @Test
  void aGroupLookupNeverReturnsAnEntity() {
    GroupResolver group = factory().createByGroupAnchor("target-page");

    assertThat(group.isEmpty()).as("the anchor of an entity does not address a group").isTrue();
  }

  @Test
  void anUnknownAnchorYieldsAnEmptySelfRootedResolver() {
    EntityResolver entity = factory().createByEntityAnchor("does-not-exist");

    assertThat(entity.isEmpty()).as("an unknown anchor resolves to nothing").isTrue();
    assertThat(entity.globalRoot())
        .as("the empty resolver of a standalone lookup is its own root")
        .isSameAs(entity);
  }

  @Test
  void anUnknownIdKeepsThePathOfThePathBoundLookup() {
    ScenarioContext context = ScenarioContext.load(SCENARIO, LAYOUT);
    Resolver source = context.resolver("1000");

    EntityResolver entity = context.rootResolverFactory().createByEntityId(source.path(), 4711);

    assertThat(entity.isEmpty()).as("an unknown id resolves to nothing").isTrue();
    assertThat(entity.path()).as("the navigation history is kept").isSameAs(source.path());
  }

  @Test
  void aStandaloneLookupStartsItsOwnResolverTree() {
    EntityResolver entity = factory().createByEntityId(1330);

    assertThat(entity.path().segments())
        .as("a standalone root starts a fresh, single-segment path")
        .hasSize(1);
    assertThat(entity.globalRoot()).as("it is its own global root").isSameAs(entity);
  }

  @Test
  void aPathBoundLookupAppendsANewRootToTheCallersPath() {
    ScenarioContext context = ScenarioContext.load(SCENARIO, LAYOUT);
    Resolver source = context.resolver("1000");

    EntityResolver entity = context.rootResolverFactory().createByEntityId(source.path(), 1330);

    assertThat(entity.path().segments())
        .as("the new root is appended to the path of the calling object")
        .hasSize(source.path().size() + 1);
    assertThat(entity.globalRoot())
        .as("it still becomes the global root of the continued path")
        .isSameAs(entity);
  }

  @Test
  void duplicateAnchorsAreRejected() {
    Map<String, Object> entries =
        Map.of(
            "1000", Map.of("id", 1000, "anchor", "start-page"),
            "1001", Map.of("id", 1001, "anchor", "start-page"));

    assertThatThrownBy(() -> new Repository(entries))
        .as("an anchor addresses exactly one object, so a duplicate is a fixture error")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("start-page");
  }

  @Test
  void reportsAGroupAsAMicrositeRootWhenItsBlockSaysSo() {
    GroupResolver group = factory().createByGroupId(5100);

    assertThat(group.entity().isMicrositeRootSite())
        .as("the microsite marker of the group block is exposed")
        .isTrue();
  }

  @Test
  void eachLookupYieldsAFreshInstance() {
    RootResolverFactory factory = factory();

    assertThat(factory.createByEntityId(1330))
        .as("no aggregator state is shared between two lookups of the same object")
        .isNotSameAs(factory.createByEntityId(1330));
  }
}
