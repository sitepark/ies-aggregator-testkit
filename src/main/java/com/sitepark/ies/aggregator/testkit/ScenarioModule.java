package com.sitepark.ies.aggregator.testkit;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.sitepark.ies.aggregator.AggregatorErrorHandler;
import com.sitepark.ies.aggregator.AggregatorException;
import com.sitepark.ies.aggregator.output.DomainObjectMapper;
import com.sitepark.ies.aggregator.port.AssemblerFactory;
import com.sitepark.ies.aggregator.port.Channel;
import com.sitepark.ies.aggregator.port.ChannelProvider;
import com.sitepark.ies.aggregator.port.ImageScaler;
import com.sitepark.ies.aggregator.port.MediaProvider;
import com.sitepark.ies.aggregator.port.ObjectTypeConfigProvider;
import com.sitepark.ies.aggregator.port.VariantConfigProvider;
import com.sitepark.ies.aggregator.resolver.RootResolverFactory;
import com.sitepark.ies.aggregator.value.StructuredValueParser;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Binds the ports a scenario stands in for, so the injector can build every assembler and every
 * collaborator from their {@code @Inject} constructors.
 *
 * <p>This mirrors the production {@code AggregatorModule}: only ports are declared here, everything
 * concrete — services, resolvers, builders, dispatchers — is reached through just-in-time bindings.
 * A new assembler with a new collaborator therefore needs no change to the testkit; that is the
 * whole point of this module, and what lets a custom-extension project reuse the harness for its own
 * assemblers.
 *
 * <p>Three bindings are deliberately <em>not</em> what production binds, and each buys a property a
 * golden file needs:
 *
 * <ul>
 *   <li>the {@link Clock} is frozen, because an expectation cannot carry a moving timestamp,
 *   <li>the {@link AggregatorErrorHandler} throws instead of logging — production is lenient on
 *       purpose, the harness must not be, or a scenario stays green while the published page loses a
 *       section,
 *   <li>the {@link ImageScaler} is the deterministic {@link ScenarioImageScaler}.
 * </ul>
 */
final class ScenarioModule implements Module {

  /**
   * The clock every assembler reads the current time from. Pinned, because a golden file cannot
   * carry a moving timestamp: an assembler that stamps the generation time would otherwise differ on
   * every run.
   */
  static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneOffset.UTC);

  private final AssemblerPackages assemblerPackages;
  private final MediaProvider mediaProvider;
  private final StructuredValueParser parser;
  private final ChannelProvider channelProvider;
  private final VariantConfigProvider variantConfigProvider;
  private final RootResolverFactory rootResolverFactory;
  private final ObjectTypeConfigProvider objectTypeConfigProvider;

  ScenarioModule(
      AssemblerPackages assemblerPackages,
      MediaProvider mediaProvider,
      StructuredValueParser parser,
      ChannelProvider channelProvider,
      VariantConfigProvider variantConfigProvider,
      RootResolverFactory rootResolverFactory,
      ObjectTypeConfigProvider objectTypeConfigProvider) {
    this.assemblerPackages = assemblerPackages;
    this.mediaProvider = mediaProvider;
    this.parser = parser;
    this.channelProvider = channelProvider;
    this.variantConfigProvider = variantConfigProvider;
    this.rootResolverFactory = rootResolverFactory;
    this.objectTypeConfigProvider = objectTypeConfigProvider;
  }

  @Override
  public void configure(Binder binder) {
    // Singleton, because the factory is injected into the dispatchers it hands out itself; they all
    // have to see the same instance, and the classpath scan is worth doing once per scenario.
    binder.bind(AssemblerFactory.class).to(ScenarioAssemblerFactory.class).in(Singleton.class);
    binder.bind(AssemblerPackages.class).toInstance(this.assemblerPackages);

    binder.bind(MediaProvider.class).toInstance(this.mediaProvider);
    binder.bind(StructuredValueParser.class).toInstance(this.parser);
    binder.bind(ChannelProvider.class).toInstance(this.channelProvider);
    binder.bind(Channel.class).toInstance(this.channelProvider.current());
    binder.bind(VariantConfigProvider.class).toInstance(this.variantConfigProvider);
    binder.bind(RootResolverFactory.class).toInstance(this.rootResolverFactory);
    binder.bind(ObjectTypeConfigProvider.class).toInstance(this.objectTypeConfigProvider);
    binder.bind(DomainObjectMapper.class).to(ReflectiveDomainObjectMapper.class);

    binder.bind(ImageScaler.class).toInstance(new ScenarioImageScaler());
    binder.bind(Clock.class).toInstance(FIXED_CLOCK);
    binder.bind(AggregatorErrorHandler.class).toInstance(ScenarioModule::fail);
  }

  /**
   * A scenario is a fixture, not live data: an aggregation error is always a test failure, never
   * something to skip past.
   */
  private static void fail(AggregatorException e) {
    throw new AssertionError("aggregation failed in a scenario", e);
  }
}
