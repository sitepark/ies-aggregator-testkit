package com.sitepark.ies.aggregator.testkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.Guice;
import com.sitepark.ies.aggregator.port.AssemblerChain;
import com.sitepark.ies.aggregator.port.AssemblerFactory;
import com.sitepark.ies.aggregator.port.Channel;
import com.sitepark.ies.aggregator.port.ChannelProvider;
import com.sitepark.ies.aggregator.port.MediaProvider;
import com.sitepark.ies.aggregator.port.ObjectTypeConfigProvider;
import com.sitepark.ies.aggregator.port.VariantConfigProvider;
import com.sitepark.ies.aggregator.resolver.Resolver;
import com.sitepark.ies.aggregator.resolver.ResolverPath;
import com.sitepark.ies.aggregator.resolver.RootResolverFactory;
import com.sitepark.ies.aggregator.testkit.chainfixture.ChainProbe;
import com.sitepark.ies.aggregator.value.StructuredValueParser;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Verifies context-based assembler selection in {@link ScenarioAssemblerFactory}: filtering by the
 * {@code objectType} derived from the resolver's current scope (including the switch across a link
 * boundary) and by a custom {@link com.sitepark.ies.aggregator.AssemblerCondition}. The probe
 * assemblers live in the {@code chainfixture} package under their own keys, so they never interfere
 * with the production scenario tests.
 */
class ObjectTypeAssemblerFactoryTest {

  /** A provider whose {@code current()} answers a channel, as the port's contract requires. */
  private static ChannelProvider channelProvider() {
    ChannelProvider provider = mock(ChannelProvider.class);
    when(provider.current()).thenReturn(mock(Channel.class));
    return provider;
  }

  /**
   * The factory as a scenario wires it, but pointed at the probe package and given mocked ports —
   * the probes have no collaborators, so nothing is ever resolved through them.
   */
  private static AssemblerFactory factory() {
    return Guice.createInjector(
            new ScenarioModule(
                AssemblerPackages.of("com.sitepark.ies.aggregator.testkit.chainfixture"),
                mock(MediaProvider.class),
                mock(StructuredValueParser.class),
                channelProvider(),
                mock(VariantConfigProvider.class),
                mock(RootResolverFactory.class),
                mock(ObjectTypeConfigProvider.class)))
        .getInstance(AssemblerFactory.class);
  }

  /** A root resolver of a single object of the given object type. */
  private static Resolver object(String objectType) {
    Repository repository =
        new Repository(Map.of("1", Map.of("id", "1", "objectType", objectType)));
    return ResolverPath.createRoot(path -> repository.resolver(path, "1"));
  }

  /** A resolver on a {@code news} article reached by following a link from a {@code page}. */
  private static Resolver newsLinkedFromPage() {
    Repository repository =
        new Repository(
            Map.of(
                "1", Map.of("id", "1", "objectType", "page", "content", Map.of("sp_link", "2")),
                "2", Map.of("id", "2", "objectType", "news")));
    Resolver page = ResolverPath.createRoot(path -> repository.resolver(path, "1"));
    return page.resolveLink("sp_link");
  }

  private static List<String> names(AssemblerChain<ChainProbe> chain) {
    return chain.assemblers().stream().map(ChainProbe::name).toList();
  }

  @Test
  void includesWildcardAndMatchingObjectTypeAssemblers() {
    AssemblerChain<ChainProbe> chain =
        factory().createChain("chain.otype", ChainProbe.class, object("news"));

    assertThat(names(chain))
        .as("a news object gets the wildcard base plus the news-specific assembler")
        .containsExactly("base", "news");
  }

  @Test
  void excludesObjectTypeSpecificAssemblerForOtherType() {
    AssemblerChain<ChainProbe> chain =
        factory().createChain("chain.otype", ChainProbe.class, object("event"));

    assertThat(names(chain))
        .as("an event object gets only the wildcard base, not the news-specific assembler")
        .containsExactly("base");
  }

  @Test
  void keepsOnlyWildcardWhenScopeHasNoObjectType() {
    AssemblerChain<ChainProbe> chain =
        factory().createChain("chain.otype", ChainProbe.class, Resolver.empty());

    assertThat(names(chain))
        .as("without an entity scope only wildcard assemblers apply")
        .containsExactly("base");
  }

  @Test
  void filtersByLinkedObjectTypeAcrossLinkBoundary() {
    AssemblerChain<ChainProbe> chain =
        factory().createChain("chain.otype", ChainProbe.class, newsLinkedFromPage());

    assertThat(names(chain))
        .as("assemblers run on a linked article are filtered by the linked article's object type")
        .containsExactly("base", "news");
  }

  @Test
  void createSelectsHighestPriorityEligibleAssembler() {
    ChainProbe probe = factory().create("chain.otype", ChainProbe.class, object("news"));

    assertThat(probe.name())
        .as("create picks the highest-priority eligible assembler for the object type")
        .isEqualTo("news");
  }

  @Test
  void conditionAppliesWhenContextMatches() {
    AssemblerChain<ChainProbe> chain =
        factory().createChain("chain.cond", ChainProbe.class, object("news"));

    assertThat(names(chain))
        .as("the gated assembler applies when its condition matches the news scope")
        .containsExactly("base", "gated");
  }

  @Test
  void conditionExcludesAssemblerWhenContextDoesNotMatch() {
    AssemblerChain<ChainProbe> chain =
        factory().createChain("chain.cond", ChainProbe.class, object("event"));

    assertThat(names(chain))
        .as("the gated assembler is excluded when its condition does not match")
        .containsExactly("base");
  }
}
