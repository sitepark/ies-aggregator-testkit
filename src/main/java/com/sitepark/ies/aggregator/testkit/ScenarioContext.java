package com.sitepark.ies.aggregator.testkit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.sitepark.ies.aggregator.Aggregator;
import com.sitepark.ies.aggregator.Options;
import com.sitepark.ies.aggregator.OptionsAware;
import com.sitepark.ies.aggregator.RootAggregator;
import com.sitepark.ies.aggregator.output.Component;
import com.sitepark.ies.aggregator.output.DomainObjectMapper;
import com.sitepark.ies.aggregator.output.OutputList;
import com.sitepark.ies.aggregator.output.OutputNode;
import com.sitepark.ies.aggregator.output.OutputObject;
import com.sitepark.ies.aggregator.output.convert.MapConverter;
import com.sitepark.ies.aggregator.output.format.JsonWriter;
import com.sitepark.ies.aggregator.port.AssemblerFactory;
import com.sitepark.ies.aggregator.port.Channel;
import com.sitepark.ies.aggregator.port.ChannelProvider;
import com.sitepark.ies.aggregator.resolver.Resolver;
import com.sitepark.ies.aggregator.resolver.ResolverPath;
import com.sitepark.ies.aggregator.resolver.RootResolverFactory;
import com.sitepark.ies.aggregator.value.AccessRestriction;
import com.sitepark.ies.aggregator.value.StructuredValueParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Integration-test harness for component aggregators.
 *
 * <p>It loads a JSON scenario (a normalized {@link Repository}) and wires the full, real assembler
 * chain around it: a {@link RepositoryResolver} as the CMS view, a {@link RepositoryMediaProvider}
 * for media loaded across link boundaries, a Jackson-backed {@link StructuredValueParser} for
 * embedded structured values, a {@link ScenarioRootResolverFactory} for objects addressed by id or
 * anchor instead of being linked, and a {@link ScenarioAssemblerFactory} that runs the production
 * assembler classes unmodified. Only the ports are named here; the assemblers and their
 * collaborators are built by an injector from their {@code @Inject} constructors, as production does
 * it — see {@link ScenarioModule}.
 *
 * <p>The generated resource is serialized with the production {@link JsonWriter} and compared as
 * normalized (key-sorted, pretty) JSON against a golden file, so the assertion covers the whole
 * output of an aggregation at once — including what it writes outside the content area.
 */
// The harness wires the whole production chain around a scenario and drives both kinds of
// aggregator, so it names a lot of collaborators by design.
@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.ExcessiveImports"})
public final class ScenarioContext {

  /**
   * Base packages scanned for {@code @AssemblerBinding} classes by default. {@code com.sitepark}
   * covers the built-ins of the consuming project as well as any custom-extension project
   * underneath (e.g. {@code com.sitepark.stuttgart.aggregator}); use {@link #load(String,
   * ScenarioLayout, String...)} to narrow or replace this.
   */
  public static final List<String> DEFAULT_ASSEMBLER_PACKAGES = List.of("com.sitepark");

  /**
   * Top-level key in a scenario file carrying the aggregator options. Retrieve the deserialized
   * options via {@link #options(Class)}.
   */
  public static final String OPTIONS_KEY = "options";

  /**
   * Top-level key in a scenario file carrying the access restriction the channel reports for the
   * resource: {@code {"mode": "ALLOW"|"DENY", "groups": [...]}}. An absent block means the resource
   * is unrestricted.
   */
  public static final String ACCESS_KEY = "access";

  /**
   * Top-level key in a scenario file carrying the configuration of the resource's object type —
   * whether it is a home page, kept out of the index, and so on. A scenario describes one resource,
   * so it configures one type. An absent block means the type is not configured.
   */
  public static final String OBJECT_TYPE_KEY = "objectType";

  /**
   * Top-level key in a scenario file carrying the {@link Repository}: a flat map {@code id ->
   * entry} of the objects the aggregation navigates (the section source and any linked objects).
   */
  public static final String REPOSITORY_KEY = "repository";

  /**
   * Top-level key in a scenario file carrying the image-variant catalog: a map {@code variant name
   * -> variant configuration}, standing in for what the platform's variant-config service answers.
   */
  public static final String VARIANT_CONFIG_KEY = "variantConfig";

  /**
   * Id of the repository entry the aggregation starts from in every scenario (by convention
   * {@code 1000}). {@link #aggregate} resolves it as the entry point of the aggregation and reuses
   * it as the id of the aggregated component.
   */
  public static final String SOURCE_ID = "1000";

  private static final String ITEMS = "items";
  private static final String TYPE = "type";
  private static final String ID = "id";

  private final Repository repository;
  private final ScenarioLayout layout;
  private final Injector injector;
  private final AssemblerFactory assemblerFactory;
  private final RootResolverFactory rootResolverFactory;
  private final ChannelProvider channelProvider;
  private final ObjectMapper jsonMapper;
  private final @Nullable Object rawOptions;

  private ScenarioContext(
      Repository repository,
      ScenarioLayout layout,
      Injector injector,
      RootResolverFactory rootResolverFactory,
      ChannelProvider channelProvider,
      ObjectMapper jsonMapper,
      @Nullable Object rawOptions) {
    this.repository = repository;
    this.layout = layout;
    this.injector = injector;
    this.assemblerFactory = injector.getInstance(AssemblerFactory.class);
    this.rootResolverFactory = rootResolverFactory;
    this.channelProvider = channelProvider;
    this.jsonMapper = jsonMapper;
    this.rawOptions = rawOptions;
  }

  /**
   * Loads a scenario resource and wires the assembler chain around it, discovering assemblers in
   * the {@link #DEFAULT_ASSEMBLER_PACKAGES}.
   *
   * @param resource the classpath resource of the scenario data (e.g. {@code
   *     "scenarios/content-artdirection/linked-media-ignore-copyright.json"})
   * @param layout where the consuming project's resource keeps its components
   */
  public static ScenarioContext load(String resource, ScenarioLayout layout) {
    return load(resource, layout, DEFAULT_ASSEMBLER_PACKAGES.toArray(String[]::new));
  }

  /**
   * Loads a scenario resource and wires the assembler chain around it, discovering assemblers under
   * the given base packages.
   *
   * <p>Custom-extension projects reusing this testkit point it at their own package(s) (e.g. {@code
   * "com.sitepark.stuttgart.aggregator"}); their overriding assemblers still win by
   * {@code @AssemblerBinding} priority. Include the package of the library they extend too if its
   * built-ins are needed alongside.
   *
   * @param resource the classpath resource of the scenario data
   * @param layout where the consuming project's resource keeps its components
   * @param assemblerPackages the base packages to scan for {@code @AssemblerBinding} classes
   */
  public static ScenarioContext load(
      String resource, ScenarioLayout layout, String... assemblerPackages) {
    // Unknown properties are ignored, mirroring the production IES mapper. A real section-type
    // configuration carries plenty of keys no aggregator reads, so a scenario must be able to hold
    // such a configuration verbatim.
    // Enum constants are written the way they leave the aggregator — "allow", not "ALLOW" — so a
    // scenario reads like the output it produces.
    ObjectMapper mapper =
        new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
    Map<String, Object> raw = read(mapper, resource, new TypeReference<Map<String, Object>>() {});

    // A scenario has two top-level areas: the optional options block and the repository.
    Object rawOptions = raw.get(OPTIONS_KEY);
    Repository repository = new Repository(repositoryEntries(resource, raw));

    StructuredValueParser parser =
        new StructuredValueParser() {
          @Override
          public <T> T parse(String raw, Class<T> type) {
            try {
              return mapper.readValue(raw, type);
            } catch (IOException e) {
              throw new UncheckedIOException("Cannot parse structured value: " + raw, e);
            }
          }
        };

    RootResolverFactory rootResolverFactory = new ScenarioRootResolverFactory(repository);
    ChannelProvider channelProvider =
        new ScenarioChannelProvider(repository, accessRestriction(mapper, raw));
    // Only the ports are wired by hand; every assembler and every collaborator behind them is built
    // by the injector from its @Inject constructor, the way the production container does it.
    Injector injector =
        Guice.createInjector(
            new ScenarioModule(
                new AssemblerPackages(Arrays.asList(assemblerPackages)),
                new RepositoryMediaProvider(),
                parser,
                channelProvider,
                new ScenarioVariantConfigProvider(mapper, raw.get(VARIANT_CONFIG_KEY)),
                rootResolverFactory,
                new ScenarioObjectTypeConfigProvider(mapper, raw.get(OBJECT_TYPE_KEY))));

    return new ScenarioContext(
        repository, layout, injector, rootResolverFactory, channelProvider, mapper, rawOptions);
  }

  /**
   * Reads the scenario's access restriction; an absent block means the resource is unrestricted.
   */
  private static @Nullable AccessRestriction accessRestriction(
      ObjectMapper mapper, Map<String, Object> raw) {
    Object access = raw.get(ACCESS_KEY);
    return access == null ? null : mapper.convertValue(access, AccessRestriction.class);
  }

  /**
   * Builds the aggregator under test the way the production container builds it: from its
   * {@code @Inject} constructor, with the harness's ports and assembler factory injected.
   *
   * <p>This is how a project reaches an aggregator of a library it extends - their constructors are
   * package-private on purpose, as production never calls them either. An aggregator that declares
   * {@link OptionsAware} is handed the scenario's options as well, again as production does it, so
   * a test never sets them by hand.
   *
   * @param type the aggregator class
   * @param <T> the aggregator type
   */
  public <T> T aggregator(Class<T> type) {
    T aggregator = this.injector.getInstance(type);
    if (aggregator instanceof OptionsAware<?> optionsAware) {
      this.setOptions(optionsAware);
    }
    return aggregator;
  }

  /**
   * Hands the aggregator the scenario's options, the way the production container does: the option
   * type comes from the {@link OptionsAware} declaration, the values from the scenario's {@code
   * options} block.
   */
  private <O extends Options> void setOptions(OptionsAware<O> optionsAware) {
    @SuppressWarnings("unchecked")
    Class<O> optionType = (Class<O>) optionTypeOf(optionsAware.getClass());
    if (optionType != null) {
      optionsAware.setOptions(this.options(optionType));
    }
  }

  /**
   * The type argument the class declares for {@link OptionsAware}, walking up its superclasses - an
   * aggregator declares it on itself, but may inherit the declaration.
   *
   * @return the option type, or {@code null} if the declaration is raw
   */
  private static @Nullable Class<?> optionTypeOf(Class<?> aggregatorType) {
    for (Class<?> current = aggregatorType; current != null; current = current.getSuperclass()) {
      for (Type declared : current.getGenericInterfaces()) {
        if (declared instanceof ParameterizedType parameterized
            && parameterized.getRawType() == OptionsAware.class
            && parameterized.getActualTypeArguments()[0] instanceof Class<?> optionType) {
          return optionType;
        }
      }
    }
    return null;
  }

  /** Returns the wired assembler factory to hand to the aggregator under test. */
  public AssemblerFactory assemblerFactory() {
    return this.assemblerFactory;
  }

  /**
   * Returns the domain-object mapper the production writers use, for aggregators that map an area
   * model onto its area node.
   */
  public DomainObjectMapper domainObjectMapper() {
    return new ReflectiveDomainObjectMapper();
  }

  /**
   * Returns the root-resolver factory the assemblers are wired with, for tests that address objects
   * of the scenario by id or anchor themselves.
   */
  public RootResolverFactory rootResolverFactory() {
    return this.rootResolverFactory;
  }

  /**
   * Deserializes the scenario's {@code options} block into the given options type, mirroring the
   * production {@code ConfigMapper} path ({@link ObjectMapper#convertValue}). When the scenario has
   * no {@code options} block, an empty options instance is returned so aggregation runs with the
   * default behaviour.
   *
   * @param type the concrete options type to deserialize into
   */
  public <T extends Options> T options(Class<T> type) {
    return this.jsonMapper.convertValue(this.rawOptions == null ? Map.of() : this.rawOptions, type);
  }

  /**
   * Returns the section source resolver for the given entry id.
   *
   * @param id the id of the entry that acts as the section source
   */
  public Resolver resolver(String id) {
    return ResolverPath.createRoot(path -> this.repository.resolver(path, id));
  }

  /**
   * Aggregates the scenario's source the way a content container does and returns the complete
   * generated resource as normalized JSON.
   *
   * <p>The component handed to the aggregator sits where it sits in production: in the container
   * of the content area the layout names. And as in production the component is attached only if
   * the aggregator reports that it produced content, so the golden file always shows the resource a
   * page would really carry:
   *
   * <ul>
   *   <li>an aggregation writing into its component yields the content area,
   *   <li>an aggregation writing only outside of it — into a page-wide area that describes the page
   *       for listings, say — yields a resource without a content area,
   *   <li>an aggregation producing nothing at all yields an empty resource.
   * </ul>
   *
   * @param aggregation the aggregator's own component method, e.g. {@code
   *     aggregator::aggregateSectionType}
   * @param componentType the type of the component the aggregation aggregates into
   * @return the whole resource as normalized JSON, comparable with {@link #expected(String)}
   */
  public String aggregate(ComponentAggregation aggregation, String componentType) {

    OutputObject root = new OutputObject(null, null);

    // Built detached from the root and hooked in below only if the aggregation produced content,
    // so an empty content area never shows up in the resource.
    OutputObject contentRoot = new OutputObject(this.layout.contentArea(), root);
    contentRoot.put(TYPE, this.layout.rootMarker());
    contentRoot.put(ID, this.layout.rootMarker());
    OutputList containers = contentRoot.nodeList(ITEMS);
    Component container =
        new Component(containers, this.layout.containerType(), this.layout.containerId());
    Component component = container.newComponent(componentType, SOURCE_ID);

    if (aggregation.aggregate(this.resolver(SOURCE_ID), component)) {
      root.put(this.layout.contentArea(), contentRoot);
      containers.addItem(container);
      container.addComponent(component);
    }

    return toJson(root);
  }

  /**
   * Aggregates the scenario's source the way the {@code sp:aggregator} tag does and returns the
   * complete generated resource as normalized JSON.
   *
   * <p>This is the path that is live in production, and it differs from {@link
   * #aggregate(ComponentAggregation, String)} in exactly the place where it matters: the
   * aggregator is not asked for its section directly but driven through {@link
   * Aggregator#aggregate}, handed a plain node instead of the component in the tree, and whether
   * the section reaches the page is decided the way {@code AggregatorTag} decides it — by whether
   * the aggregator wrote anything at all into that node, not by what it reported. The parameter is
   * therefore the plain aggregator port: this path never calls the aggregator's own component method.
   *
   * <p>That difference is what the reference channel exposed: an entry written unconditionally
   * makes the node non-empty, so an aggregator that produced nothing still lands in the page.
   *
   * <p>The rest mirrors the tag as well. Entries that render empty are dropped on the way into the
   * component — the tag converts the node to a map first, and that conversion is what keeps the
   * empty {@code type} and {@code id} of the throwaway component from overwriting the ones SPML
   * had already put there. What the aggregator wrote into other areas is lifted onto the resource.
   *
   * @param aggregator the configured aggregator under test
   * @param componentType the type of the component the aggregation aggregates into
   * @return the whole resource as normalized JSON, comparable with {@link #expected(String)}
   */
  public String aggregateAsSection(Aggregator aggregator, String componentType) {

    OutputObject root = new OutputObject(null, null);

    // As in aggregate(ComponentAggregation, String): built detached and hooked in below only if
    // the section reaches the page.
    OutputObject contentRoot = new OutputObject(this.layout.contentArea(), root);
    contentRoot.put(TYPE, this.layout.rootMarker());
    contentRoot.put(ID, this.layout.rootMarker());
    OutputList containers = contentRoot.nodeList(ITEMS);
    Component container =
        new Component(containers, this.layout.containerType(), this.layout.containerId());
    Component section = container.newComponent(componentType, SOURCE_ID);

    // The node the tag hands in: a plain object on a root of its own, so whatever an aggregator
    // writes beside its own output lands next to it and can be lifted onto the resource.
    OutputObject tagRoot = new OutputObject(null, null);
    OutputObject aggregated = tagRoot.node(this.layout.contentArea());
    aggregator.aggregate(this.resolver(SOURCE_ID), aggregated);

    boolean reachesThePage = !aggregated.entries().isEmpty();

    copyRendered(aggregated, section);
    for (Map.Entry<String, Object> entry : tagRoot.entries().entrySet()) {
      if (!this.layout.contentArea().equals(entry.getKey())
          && entry.getValue() instanceof OutputObject area) {
        copyRendered(area, root.node(entry.getKey()));
      }
    }

    if (reachesThePage) {
      root.put(this.layout.contentArea(), contentRoot);
      containers.addItem(container);
      container.addComponent(section);
    }

    return toJson(root);
  }

  /**
   * Copies the entries of {@code source} into {@code target}, leaving out those the output format
   * would drop as empty.
   *
   * <p>The production {@link MapConverter} makes that decision here too, so the harness cannot
   * disagree with the writer about what is empty; only the key set of its result is used, the
   * values stay the original output nodes.
   */
  private static void copyRendered(OutputObject source, OutputNode target) {
    Map<String, Object> rendered =
        new MapConverter(new ReflectiveDomainObjectMapper()).toMap(source);
    for (Map.Entry<String, Object> entry : source.entries().entrySet()) {
      if (rendered.containsKey(entry.getKey())) {
        target.put(entry.getKey(), entry.getValue());
      }
    }
  }

  /**
   * Aggregates the scenario's source the way the {@code RootAggregator} does and returns the
   * complete generated resource as normalized JSON.
   *
   * <p>The aggregator receives the shared resource root, so it takes the branch that writes into
   * its own area — the branch that becomes the live one once SPML stops driving the generation. The
   * SPML branch writes the very same entries, only into a node the tag hands in.
   *
   * @param aggregator the configured area aggregator under test
   * @return the whole resource as normalized JSON, comparable with {@link #expected(String)}
   */
  public String aggregate(Aggregator aggregator) {
    OutputObject root = new OutputObject(null, null);
    aggregator.aggregate(this.resolver(SOURCE_ID), root);
    return toJson(root);
  }

  /**
   * Aggregates a medium uploaded into a field of the scenario's source, the way the root aggregator
   * of an embedded medium's meta file does.
   *
   * <p>The source of that aggregation is the medium, not the page carrying it — the page stays
   * reachable as the source's root. Which medium is named by its asset id, the {@code id} of the
   * {@code media} block, mirroring how the publisher names it.
   *
   * @param aggregator the configured area aggregator under test
   * @param mediaId the asset id of the upload to aggregate
   * @return the whole resource as normalized JSON, comparable with {@link #expected(String)}
   */
  public String aggregateEmbeddedMedia(Aggregator aggregator, int mediaId) {
    OutputObject root = new OutputObject(null, null);
    aggregator.aggregate(this.embeddedMedia(mediaId), root);
    return toJson(root);
  }

  /**
   * The channel the scenario publishes into.
   *
   * <p>For a test that writes a document rather than comparing aggregated data: the writer needs
   * the channel to answer what a resource path looks like.
   */
  public Channel channel() {
    return this.channelProvider.current();
  }

  /**
   * Aggregates a medium uploaded into a field of the scenario's source and hands back the tree
   * itself, for a test that writes it as a document instead of comparing it as data.
   *
   * @param aggregator the root aggregator under test
   * @param mediaId the asset id of the upload to aggregate
   */
  public OutputObject aggregateEmbeddedMediaTree(RootAggregator aggregator, int mediaId) {
    return aggregator.aggregate(this.embeddedMedia(mediaId));
  }

  /**
   * The upload with the given asset id, reached by walking the field path it sits under.
   *
   * <p>The same construction the adapter performs for the publisher: an aggregation of an embedded
   * medium starts at the upload and keeps the page as its root, and the step-by-step walk is what
   * gives the upload the node key an ordinary aggregation of that page would.
   */
  private Resolver embeddedMedia(int mediaId) {
    String field = this.repository.embeddedMediaPath(SOURCE_ID, mediaId);
    if (field == null) {
      throw new IllegalArgumentException(
          "The scenario source carries no upload with the asset id " + mediaId);
    }
    Resolver media = this.resolver(SOURCE_ID);
    for (String step : field.split("\\.", -1)) {
      media = media.resolve(step);
    }
    return media;
  }

  /** Serializes the resource to normalized JSON via the production {@link JsonWriter}. */
  private String toJson(OutputObject root) {
    StringWriter writer = new StringWriter();
    root.accept(new JsonWriter(writer, new ReflectiveDomainObjectMapper()));
    return normalize(writer.toString());
  }

  /**
   * Loads a golden output resource and returns it in the same normalized form as {@link #toJson}.
   */
  public String expected(String resource) {
    try (InputStream in = openResource(resource)) {
      return pretty(this.jsonMapper.readValue(in, Object.class));
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read expected output: " + resource, e);
    }
  }

  private String normalize(String json) {
    try {
      return pretty(this.jsonMapper.readValue(json, Object.class));
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot parse JSON: " + json, e);
    }
  }

  /** Renders the tree with recursively sorted object keys, so comparison is order-independent. */
  private String pretty(Object tree) {
    try {
      return this.jsonMapper
          .writerWithDefaultPrettyPrinter()
          .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
          .writeValueAsString(tree);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot render JSON", e);
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> repositoryEntries(String resource, Map<String, Object> raw) {
    Object repository = raw.get(REPOSITORY_KEY);
    if (!(repository instanceof Map)) {
      throw new IllegalArgumentException(
          "Scenario is missing the '" + REPOSITORY_KEY + "' object: " + resource);
    }
    return (Map<String, Object>) repository;
  }

  private static <T> T read(ObjectMapper mapper, String resource, TypeReference<T> type) {
    try (InputStream in = openResource(resource)) {
      return mapper.readValue(in, type);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read scenario: " + resource, e);
    }
  }

  // The harness and the scenarios of the project using it share one classpath, and the loader
  // that brought this class is the one that sees them; no container class loader is in play.
  @SuppressWarnings("PMD.UseProperClassLoader")
  private static InputStream openResource(String resource) {
    InputStream in = ScenarioContext.class.getClassLoader().getResourceAsStream(resource);
    if (in == null) {
      throw new IllegalArgumentException("Resource not found on classpath: " + resource);
    }
    return in;
  }
}
