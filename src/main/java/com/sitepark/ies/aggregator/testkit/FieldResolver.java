package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.resolver.Resolver;
import com.sitepark.ies.aggregator.resolver.ResolverPath;
import com.sitepark.ies.aggregator.value.ResolvedValue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link Resolver} over a plain field map, for unit tests that only read leaf values.
 *
 * <p>It exists because the default methods of {@link Resolver} — {@code coalesce}, {@code
 * resolveLink} — are what several assemblers are built on, and a Mockito mock does not run them. A
 * real implementation over a map keeps such a test about the assembler's behaviour instead of about
 * stubbing.
 *
 * <p>Navigation is out of scope: {@link #resolve(String)} and {@link #resolveList(String)} answer
 * empty. Use the scenario harness ({@link ScenarioContext}) when a test needs to cross object
 * boundaries.
 */
public final class FieldResolver implements Resolver {

  private final Map<String, Object> fields = new LinkedHashMap<>();

  private String nodeKey = "";

  /** Creates a resolver without any field. */
  public static FieldResolver empty() {
    return new FieldResolver();
  }

  /**
   * Adds a field.
   *
   * @param key the field name
   * @param value the field value
   * @return this resolver, for chaining
   */
  public FieldResolver with(String key, Object value) {
    this.fields.put(key, value);
    return this;
  }

  /**
   * Sets the key {@link #nodeKey()} answers, for a test that derives an identity from it.
   *
   * @param nodeKey the key of the node this resolver stands for
   * @return this resolver, for chaining
   */
  public FieldResolver withNodeKey(String nodeKey) {
    this.nodeKey = nodeKey;
    return this;
  }

  /** Answers what {@link #withNodeKey(String)} was given, and the empty string otherwise. */
  @Override
  public String nodeKey() {
    return this.nodeKey;
  }

  @Override
  public ResolvedValue value(String key) {
    Object value = this.fields.get(key);
    return value == null ? ResolvedValue.empty() : ResolvedValue.of(value);
  }

  @Override
  public boolean isEmpty() {
    return this.fields.isEmpty();
  }

  @Override
  public ResolverPath path() {
    return ResolverPath.of(this);
  }

  @Override
  public Resolver resolve(String key) {
    return Resolver.empty();
  }

  @Override
  public List<Resolver> resolveList(String key) {
    return List.of();
  }
}
