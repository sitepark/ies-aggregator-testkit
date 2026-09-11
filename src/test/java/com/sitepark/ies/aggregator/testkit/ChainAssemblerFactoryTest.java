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
import com.sitepark.ies.aggregator.resolver.RootResolverFactory;
import com.sitepark.ies.aggregator.testkit.chainfixture.ChainProbe;
import com.sitepark.ies.aggregator.value.StructuredValueParser;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Verifies the chain assembly of {@link ScenarioAssemblerFactory#createChain}: ascending-priority
 * ordering and the {@code chainRoot}/{@code chainBreak} pruning. The probe assemblers live in the
 * {@code chainfixture} package under their own keys, so they never interfere with the production
 * scenario tests.
 */
class ChainAssemblerFactoryTest {

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

  private static List<String> names(AssemblerChain<ChainProbe> chain) {
    return chain.assemblers().stream().map(ChainProbe::name).toList();
  }

  @Test
  void ordersChainByAscendingPriority() {
    AssemblerChain<ChainProbe> chain = factory().createChain("chain.order", ChainProbe.class);

    assertThat(names(chain))
        .as("chain runs the built-in (priority 0) first, higher-priority enrichers after")
        .containsExactly("base", "top");
  }

  @Test
  void chainRootAndChainBreakPruneToTheirWindow() {
    AssemblerChain<ChainProbe> chain = factory().createChain("chain.cut", ChainProbe.class);

    // chain.cut has priorities 0 (bottom), 5 (root, chainRoot), 7 (brk, chainBreak), 10 (top).
    // chainRoot drops everything below priority 5, chainBreak drops everything above priority 7.
    assertThat(names(chain))
        .as("chainRoot skips lower-priority and chainBreak skips higher-priority assemblers")
        .containsExactly("root", "brk");
  }

  @Test
  void anAssemblerBringsItsOwnCollaborator() {
    ChainProbe probe = factory().create("chain.collaborator", ChainProbe.class);

    assertThat(probe.name())
        .as("an assembler's own collaborator is built by the injector, unknown to the testkit")
        .isEqualTo("channel");
  }

  @Test
  void emptyWindowYieldsEmptyChain() {
    AssemblerChain<ChainProbe> chain = factory().createChain("chain.empty", ChainProbe.class);

    assertThat(chain.isEmpty())
        .as("a chainBreak below a chainRoot leaves no assembler in the window")
        .isTrue();
  }
}
