package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.AggregatorErrorHandler;
import com.sitepark.ies.aggregator.AggregatorException;
import com.sitepark.ies.aggregator.output.DomainObjectMapper;
import com.sitepark.ies.aggregator.port.AssemblerFactory;
import com.sitepark.ies.aggregator.port.CategoryRootProvider;
import com.sitepark.ies.aggregator.port.Channel;
import com.sitepark.ies.aggregator.port.ChannelProvider;
import com.sitepark.ies.aggregator.port.ContainerHierarchy;
import com.sitepark.ies.aggregator.port.ContentNotificationRecipientProvider;
import com.sitepark.ies.aggregator.port.GlobalConfigProvider;
import com.sitepark.ies.aggregator.port.HomepageProvider;
import com.sitepark.ies.aggregator.port.IdSequence;
import com.sitepark.ies.aggregator.port.ImageScaler;
import com.sitepark.ies.aggregator.port.MediaProvider;
import com.sitepark.ies.aggregator.port.ModuleDependencies;
import com.sitepark.ies.aggregator.port.ObjectTypeConfigProvider;
import com.sitepark.ies.aggregator.port.ObjectTypeEntities;
import com.sitepark.ies.aggregator.port.PublishedLinks;
import com.sitepark.ies.aggregator.port.Regeneration;
import com.sitepark.ies.aggregator.port.ResourceState;
import com.sitepark.ies.aggregator.port.VariantConfigProvider;
import com.sitepark.ies.aggregator.port.ZipArchives;
import com.sitepark.ies.aggregator.resolver.RootResolverFactory;
import com.sitepark.ies.aggregator.value.StructuredValueParser;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.function.Consumer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Declares the ports a scenario stands in for, so the container can build every assembler and every
 * collaborator from their {@code @Inject} constructors.
 *
 * <p>This mirrors the production {@code AggregatorConfiguration}: only ports are declared here, everything
 * concrete — services, resolvers, builders, dispatchers — is created just in time.
 * A new assembler with a new collaborator therefore needs no change to the testkit; that is the
 * whole point of this module, and what lets a custom-extension project reuse the harness for its own
 * assemblers.
 *
 * <p>Three beans are deliberately <em>not</em> what production declares, and each buys a property a
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
@SuppressWarnings("PMD.ExcessiveImports") // declares one stand-in per port, so imports every port
final class ScenarioBeans implements Consumer<GenericApplicationContext> {

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
  private final InstallationPorts installationPorts;
  private final PublisherPorts publisherPorts;

  ScenarioBeans(
      AssemblerPackages assemblerPackages,
      MediaProvider mediaProvider,
      StructuredValueParser parser,
      ChannelProvider channelProvider,
      VariantConfigProvider variantConfigProvider,
      RootResolverFactory rootResolverFactory,
      InstallationPorts installationPorts,
      PublisherPorts publisherPorts) {
    this.assemblerPackages = assemblerPackages;
    this.mediaProvider = mediaProvider;
    this.parser = parser;
    this.channelProvider = channelProvider;
    this.variantConfigProvider = variantConfigProvider;
    this.rootResolverFactory = rootResolverFactory;
    this.installationPorts = installationPorts;
    this.publisherPorts = publisherPorts;
  }

  @Override
  public void accept(GenericApplicationContext context) {
    // Singleton, because the factory is injected into the dispatchers it hands out itself; they all
    // have to see the same instance, and the classpath scan is worth doing once per scenario.
    RootBeanDefinition assemblerFactory = new RootBeanDefinition(ScenarioAssemblerFactory.class);
    assemblerFactory.setScope(BeanDefinition.SCOPE_SINGLETON);
    context.registerBeanDefinition(AssemblerFactory.class.getName(), assemblerFactory);
    instance(context, AssemblerPackages.class, this.assemblerPackages);

    instance(context, MediaProvider.class, this.mediaProvider);
    instance(context, StructuredValueParser.class, this.parser);
    instance(context, ChannelProvider.class, this.channelProvider);
    // Prototype, so every injection asks the scope again; it refuses while an aggregator is built
    context.registerBean(
        Channel.class.getName(),
        Channel.class,
        ScenarioAggregationScope.channel(this.channelProvider.current()),
        definition -> definition.setScope(BeanDefinition.SCOPE_PROTOTYPE));
    instance(context, VariantConfigProvider.class, this.variantConfigProvider);
    instance(context, RootResolverFactory.class, this.rootResolverFactory);
    instance(
        context, ObjectTypeConfigProvider.class, this.installationPorts.objectTypeConfigProvider());
    instance(context, GlobalConfigProvider.class, this.installationPorts.globalConfigProvider());
    instance(context, HomepageProvider.class, this.installationPorts.homepageProvider());
    instance(context, CategoryRootProvider.class, this.installationPorts.categoryRootProvider());
    instance(context, ObjectTypeEntities.class, this.installationPorts.objectTypeEntities());
    instance(
        context,
        ContentNotificationRecipientProvider.class,
        this.installationPorts.contentNotificationRecipientProvider());
    instance(context, ContainerHierarchy.class, this.installationPorts.containerHierarchy());
    instance(context, ResourceState.class, this.publisherPorts.resourceState());
    instance(context, PublishedLinks.class, this.publisherPorts.publishedLinks());
    instance(context, Regeneration.class, this.publisherPorts.regeneration());
    instance(context, ModuleDependencies.class, this.publisherPorts.moduleDependencies());
    instance(context, ZipArchives.class, this.publisherPorts.zipArchives());
    instance(context, IdSequence.class, this.publisherPorts.idSequence());
    context.registerBeanDefinition(
        DomainObjectMapper.class.getName(),
        ScenarioContainer.definitionOf(ReflectiveDomainObjectMapper.class));

    instance(context, ImageScaler.class, new ScenarioImageScaler());
    instance(context, Clock.class, FIXED_CLOCK);
    instance(context, AggregatorErrorHandler.class, ScenarioBeans::fail);
  }

  /** Declares the instance as the bean of the type. */
  static <T> void instance(GenericApplicationContext context, Class<T> type, T instance) {
    context.registerBean(type.getName(), type, () -> instance);
  }

  /**
   * A scenario is a fixture, not live data: an aggregation error is always a test failure, never
   * something to skip past.
   */
  private static void fail(AggregatorException e) {
    throw new AssertionError("aggregation failed in a scenario", e);
  }
}
