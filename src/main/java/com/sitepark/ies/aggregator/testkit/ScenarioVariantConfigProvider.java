package com.sitepark.ies.aggregator.testkit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitepark.ies.aggregator.port.VariantConfigProvider;
import com.sitepark.ies.aggregator.value.media.scaling.VariantConfig;
import com.sitepark.ies.aggregator.value.media.scaling.VariantConfig.FormatConfig;
import com.sitepark.ies.aggregator.value.media.scaling.VariantConfig.SizeConfig;
import com.sitepark.ies.aggregator.value.media.scaling.VariantConfig.SrcSetConfig;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * {@link VariantConfigProvider} backed by the scenario's {@code variantConfig} block, standing in
 * for the platform adapter that reads the variant catalog from the project configuration.
 *
 * <p>It reads the block itself rather than being handed a parsed catalog, and it reads it the way an
 * adapter has to: the configuration speaks in tokens ({@code "cover"}, {@code "3x2"}, {@code
 * "avif"}), and {@link VariantConfig#of} is what turns them into values. Binding the JSON straight
 * onto the record would skip exactly that step.
 */
final class ScenarioVariantConfigProvider implements VariantConfigProvider {

  private static final String FIT_MODE = "fitMode";
  private static final String ASPECT_RATIO = "aspectRatio";
  private static final String FORMATS = "formats";
  private static final String SRCSET = "srcset";
  private static final String SIZES = "sizes";

  private final Map<String, VariantConfig> variants;

  /**
   * @param mapper the scenario's object mapper
   * @param block the scenario's {@code variantConfig} block, or {@code null} when it has none - then
   *     no variant is configured
   */
  ScenarioVariantConfigProvider(ObjectMapper mapper, @Nullable Object block) {
    this.variants = block == null ? Map.of() : read(mapper, block);
  }

  private static Map<String, VariantConfig> read(ObjectMapper mapper, Object block) {
    Map<String, Map<String, Object>> raw =
        mapper.convertValue(block, new TypeReference<Map<String, Map<String, Object>>>() {});
    Map<String, VariantConfig> variants = new LinkedHashMap<>();
    raw.forEach((name, config) -> variants.put(name, variant(config)));
    return variants;
  }

  private static VariantConfig variant(Map<String, Object> config) {
    return VariantConfig.of(
        (String) config.get(FIT_MODE),
        (String) config.get(ASPECT_RATIO),
        entries(
            config.get(FORMATS),
            entry ->
                FormatConfig.of((String) required(entry, "type"), (Boolean) entry.get("default"))),
        entries(
            config.get(SRCSET),
            entry -> SrcSetConfig.of(((Number) required(entry, "width")).intValue())),
        entries(
            config.get(SIZES),
            entry ->
                SizeConfig.of(
                    (String) entry.get("mediaQuery"), (String) required(entry, "displaySize"))));
  }

  /**
   * A field the configuration language does not let an entry leave out; a scenario that omits it
   * is a broken fixture, not a variant with a default.
   */
  private static Object required(Map<String, Object> entry, String key) {
    Object value = entry.get(key);
    if (value == null) {
      throw new IllegalArgumentException("variantConfig entry is missing '" + key + "': " + entry);
    }
    return value;
  }

  /** Maps the entries of a configuration list, treating an absent list as an empty one. */
  @SuppressWarnings("unchecked")
  private static <T> List<T> entries(
      @Nullable Object raw, Function<Map<String, Object>, T> factory) {
    if (raw == null) {
      return List.of();
    }
    return ((List<Map<String, Object>>) raw).stream().map(factory).toList();
  }

  @Override
  public Map<String, VariantConfig> get(Collection<String> variantNames) {
    Map<String, VariantConfig> found = new LinkedHashMap<>();
    for (String variantName : variantNames) {
      VariantConfig variant = this.variants.get(variantName);
      if (variant != null) {
        found.put(variantName, variant);
      }
    }
    return found;
  }
}
