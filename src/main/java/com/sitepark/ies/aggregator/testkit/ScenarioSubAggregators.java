package com.sitepark.ies.aggregator.testkit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import com.sitepark.ies.aggregator.Aggregator;
import com.sitepark.ies.aggregator.Configurable;
import com.sitepark.ies.aggregator.Configuration;
import com.sitepark.ies.aggregator.Options;
import com.sitepark.ies.aggregator.OptionsAware;
import com.sitepark.ies.aggregator.SectionTypeAware;
import com.sitepark.ies.aggregator.SubAggregatorAware;
import com.sitepark.ies.aggregator.SubSectionType;
import com.sitepark.ies.aggregator.SubSectionTypesAware;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Builds the children of a container from the section types it offers, the way the production
 * {@code AggregatorFactory} does it.
 *
 * <p>The offered section types are those of the section type's {@code sectionTypes}, keyed by
 * their key there, and those of its {@code fixedSectionTypes}, keyed {@code #fixed:<position>}. A
 * {@link SubAggregatorAware} aggregator gets an aggregator for each of them that runs in Java - the
 * class named by {@code aggregator.className}, if {@code classNameFor} and {@code moduleFor} do not
 * name another template - configured from that section type's own configuration, and built further
 * down the same way if it is a container itself. A section type that does not run in Java gets
 * none, as in production; the map is not handed over when it is empty. A {@link
 * SubSectionTypesAware} aggregator gets the configuration of every offered section type, read with
 * {@link SubSectionType#of}, the method the production factory reads it with.
 *
 * <p>Only the section types placed in the scenario's channel are offered at all, as the production
 * factory leaves the others out ({@code SectionType.placedIn}): a type switched off by {@code
 * enabled: false}, or not enabled for the nature of the channel by {@code aggregator.natures}, gets
 * neither an aggregator nor its configuration handed in.
 */
final class ScenarioSubAggregators {

  /** Key of the selectable section types a container offers. */
  static final String SECTION_TYPES_KEY = "sectionTypes";

  /** Key of the section types a container fixes by position. */
  static final String FIXED_SECTION_TYPES_KEY = "fixedSectionTypes";

  private static final String AGGREGATOR = "aggregator";
  private static final String TEMPLATE = "template";
  private static final String OPTIONS = "options";
  private static final String CLASS_NAME = "className";

  private final Injector injector;
  private final ObjectMapper mapper;
  private final Supplier<Optional<String>> nature;

  /**
   * @param nature the nature of the scenario's channel, asked only when a container offers section
   *     types
   */
  ScenarioSubAggregators(
      Injector injector, ObjectMapper mapper, Supplier<Optional<String>> nature) {
    this.injector = injector;
    this.mapper = mapper;
    this.nature = nature;
  }

  /**
   * Hands the aggregator the children of the given section type, if it asks for them.
   *
   * @param aggregator the aggregator built for the section type
   * @param sectionType the section type's configuration; only the section types it offers are read
   */
  void register(Object aggregator, Map<?, ?> sectionType) {
    Map<String, Map<?, ?>> offered = this.placed(offered(sectionType));
    if (aggregator instanceof SubAggregatorAware subAggregatorAware) {
      Map<String, Aggregator> children = new LinkedHashMap<>();
      for (Map.Entry<String, Map<?, ?>> child : offered.entrySet()) {
        if (runsInJava(node(child.getValue(), AGGREGATOR))) {
          children.put(child.getKey(), this.create(child.getKey(), child.getValue()));
        }
      }
      if (!children.isEmpty()) {
        subAggregatorAware.setSubAggregators(children);
      }
    }
    if (aggregator instanceof SubSectionTypesAware subSectionTypesAware) {
      Map<String, SubSectionType> types = new LinkedHashMap<>();
      for (Map.Entry<String, Map<?, ?>> child : offered.entrySet()) {
        Map<?, ?> config = child.getValue();
        types.put(
            child.getKey(),
            SubSectionType.of(
                id(child.getKey(), config), node(config, AGGREGATOR), node(config, TEMPLATE)));
      }
      subSectionTypesAware.setSubSectionTypes(types);
    }
  }

  private static Map<String, Map<?, ?>> offered(Map<?, ?> sectionType) {
    Map<String, Map<?, ?>> offered = new LinkedHashMap<>();
    if (sectionType.get(SECTION_TYPES_KEY) instanceof Map<?, ?> selectable) {
      for (Map.Entry<?, ?> entry : selectable.entrySet()) {
        if (entry.getValue() instanceof Map<?, ?> config) {
          offered.put(String.valueOf(entry.getKey()), config);
        }
      }
    }
    if (sectionType.get(FIXED_SECTION_TYPES_KEY) instanceof List<?> fixed) {
      for (int i = 0; i < fixed.size(); i++) {
        // an entry is null where the container fixes no type for that position
        if (fixed.get(i) instanceof Map<?, ?> config) {
          offered.put(SubSectionTypesAware.FIXED_KEY_PREFIX + i, config);
        }
      }
    }
    return offered;
  }

  private Map<String, Map<?, ?>> placed(Map<String, Map<?, ?>> offered) {
    if (offered.isEmpty()) {
      return offered;
    }
    Optional<String> channelNature = this.nature.get();
    offered.values().removeIf(config -> !placedIn(config, channelNature));
    return offered;
  }

  /**
   * Whether an entry of the section type is placed in a channel of the given nature, as {@code
   * SectionType.placedIn} and {@code sectionType.spml:15-22} decide it: not if {@code enabled} is
   * false; and if both the channel has a nature and the type names natures, only if it is enabled
   * for that very nature - a nature the list does not name is left out.
   */
  private static boolean placedIn(Map<?, ?> config, Optional<String> nature) {
    Object enabled = config.get("enabled");
    if (Boolean.FALSE.equals(enabled) || "false".equals(enabled)) {
      return false;
    }
    Object natures = node(config, AGGREGATOR).get("natures");
    // isNull in SPML: an absent value or an empty text is none, an empty map is not
    if (nature.isEmpty() || nature.get().isEmpty() || natures == null || "".equals(natures)) {
      return true;
    }
    return natures instanceof Map<?, ?> byNature
        && byNature.get(nature.get()) instanceof Map<?, ?> setting
        && (Boolean.TRUE.equals(setting.get("enabled")) || "true".equals(setting.get("enabled")));
  }

  /**
   * Whether a Java class applies to the section type, as {@code AggregatorConfig.runsInJava} and
   * {@code sectionType.spml} decide it: a class is named, and neither {@code classNameFor} nor
   * {@code moduleFor} names another template than the section type's own.
   */
  private static boolean runsInJava(Map<?, ?> aggregator) {
    return aggregator.get(CLASS_NAME) instanceof String className
        && !className.isBlank()
        && names(aggregator, "classNameFor", "uri")
        && names(aggregator, "moduleFor", "module");
  }

  private static boolean names(Map<?, ?> aggregator, String guard, String key) {
    Object expected = aggregator.get(guard);
    return expected == null || expected.equals(aggregator.get(key));
  }

  private Aggregator create(String key, Map<?, ?> config) {
    Map<?, ?> aggregatorNode = node(config, AGGREGATOR);
    String className = (String) Objects.requireNonNull(aggregatorNode.get(CLASS_NAME));
    Aggregator aggregator;
    try {
      Class<?> type =
          Class.forName(className, true, Thread.currentThread().getContextClassLoader());
      aggregator =
          (Aggregator) ScenarioAggregationScope.outside(() -> this.injector.getInstance(type));
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(
          "The section type '" + key + "' names a class that does not exist: " + className, e);
    } catch (ConfigurationException e) {
      throw InjectionFailure.of("Cannot build " + className, e);
    } catch (ProvisionException e) {
      throw InjectionFailure.of("Cannot build " + className, e);
    }
    if (aggregator instanceof Configurable<?> configurable) {
      this.configure(configurable, config);
    }
    if (aggregator instanceof OptionsAware<?> optionsAware) {
      this.setOptions(optionsAware, config);
    }
    if (aggregator instanceof SectionTypeAware sectionTypeAware) {
      sectionTypeAware.setSectionType(id(key, config));
    }
    this.register(aggregator, config);
    return aggregator;
  }

  /**
   * Configures a child as {@code ConfigMapper.createConfig} does: from its {@code options}, its
   * {@code template} and its {@code aggregator} node without the class name, each only if not
   * empty.
   */
  private <C extends Configuration> void configure(Configurable<C> configurable, Map<?, ?> config) {
    @SuppressWarnings("unchecked")
    Class<C> configurationType =
        (Class<C>)
            ScenarioContext.declaredTypeArgument(configurable.getClass(), Configurable.class);
    if (configurationType == null) {
      return;
    }
    Map<String, Object> configuration = new LinkedHashMap<>();
    Map<Object, Object> aggregator = new LinkedHashMap<>(node(config, AGGREGATOR));
    aggregator.remove(CLASS_NAME);
    putNotEmpty(configuration, OPTIONS, node(config, OPTIONS));
    putNotEmpty(configuration, TEMPLATE, node(config, TEMPLATE));
    putNotEmpty(configuration, AGGREGATOR, aggregator);
    configurable.configure(this.mapper.convertValue(configuration, configurationType));
  }

  private <O extends Options> void setOptions(OptionsAware<O> optionsAware, Map<?, ?> config) {
    @SuppressWarnings("unchecked")
    Class<O> optionType =
        (Class<O>)
            ScenarioContext.declaredTypeArgument(optionsAware.getClass(), OptionsAware.class);
    if (optionType != null) {
      optionsAware.setOptions(this.mapper.convertValue(node(config, OPTIONS), optionType));
    }
  }

  private static void putNotEmpty(Map<String, Object> target, String key, Map<?, ?> value) {
    if (!value.isEmpty()) {
      target.put(key, value);
    }
  }

  /** The id the configuration names the section type by, else its key. */
  private static String id(String key, Map<?, ?> config) {
    return config.get("id") instanceof String id && !id.isBlank() ? id : key;
  }

  private static Map<?, ?> node(Map<?, ?> config, String key) {
    return config.get(key) instanceof Map<?, ?> node ? node : Map.of();
  }
}
