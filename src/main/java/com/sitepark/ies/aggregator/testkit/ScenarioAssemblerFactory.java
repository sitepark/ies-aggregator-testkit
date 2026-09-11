package com.sitepark.ies.aggregator.testkit;

import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import com.sitepark.ies.aggregator.AssemblerBinding;
import com.sitepark.ies.aggregator.AssemblerCondition;
import com.sitepark.ies.aggregator.port.AssemblerChain;
import com.sitepark.ies.aggregator.port.AssemblerFactory;
import com.sitepark.ies.aggregator.resolver.EntityResolver;
import com.sitepark.ies.aggregator.resolver.Resolver;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;

/**
 * Lightweight {@link AssemblerFactory} for tests that reproduces the production lookup semantics —
 * match by {@link AssemblerBinding#value()}, highest {@link AssemblerBinding#priority()} wins —
 * without the heavy IES extension/DI runtime.
 *
 * <p>The real assembler classes run unmodified, and so do their collaborators: this factory only
 * finds the classes and picks the winner, the injector configured by {@link ScenarioModule} builds
 * them. Nothing here knows any assembler or any collaborator by name, so a new one — in this project
 * or in a custom-extension project reusing the testkit — needs no change to the testkit.
 *
 * <p>The classes are discovered via {@link ClassGraph} rather than the production class finder,
 * which walks the entries of a registered extension JAR: a test has no JAR, only
 * {@code target/test-classes}. The scan is cached per package set, so a project can point the
 * harness at its own package (e.g. {@code com.sitepark.stuttgart.aggregator}) while its overriding
 * assemblers still win by priority.
 */
final class ScenarioAssemblerFactory implements AssemblerFactory {

  /** Cached per base-package set — the classpath does not change between tests within a JVM. */
  private static final Map<Set<String>, List<Class<?>>> ASSEMBLER_CLASSES_CACHE =
      new ConcurrentHashMap<>();

  private static List<Class<?>> assemblerClasses(Collection<String> packages) {
    return ASSEMBLER_CLASSES_CACHE.computeIfAbsent(
        new TreeSet<>(packages), ScenarioAssemblerFactory::discover);
  }

  private static List<Class<?>> discover(Set<String> packages) {
    try (ScanResult scan =
        new ClassGraph()
            .enableClassInfo()
            .enableAnnotationInfo()
            .acceptPackages(packages.toArray(String[]::new))
            .scan()) {
      return scan.getClassesWithAnnotation(AssemblerBinding.class).loadClasses();
    }
  }

  private final List<Class<?>> assemblerClasses;
  private final Injector injector;

  @Inject
  ScenarioAssemblerFactory(AssemblerPackages assemblerPackages, Injector injector) {
    this.assemblerClasses = assemblerClasses(assemblerPackages.packages());
    this.injector = injector;
  }

  @Override
  public <T> T create(String key, Class<T> clazz, Resolver context) {
    String objectType = currentObjectType(context);
    Class<?> impl =
        this.assemblerClasses.stream()
            .filter(candidate -> matches(candidate, key, clazz))
            .filter(candidate -> isEligible(annotation(candidate), objectType, context))
            .max(Comparator.comparingInt(candidate -> annotation(candidate).priority()))
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No assembler registered for key '" + key + "' and type " + clazz));
    return clazz.cast(instantiate(impl));
  }

  @Override
  public <T> AssemblerChain<T> createChain(String key, Class<T> clazz, Resolver context) {
    String objectType = currentObjectType(context);
    List<Class<?>> matching =
        this.assemblerClasses.stream()
            .filter(candidate -> matches(candidate, key, clazz))
            .filter(candidate -> isEligible(annotation(candidate), objectType, context))
            .sorted(Comparator.comparingInt(candidate -> annotation(candidate).priority()))
            .toList();

    int lower = Integer.MIN_VALUE;
    int upper = Integer.MAX_VALUE;
    for (Class<?> candidate : matching) {
      AssemblerBinding ann = annotation(candidate);
      if (ann.chainRoot() && ann.priority() > lower) {
        lower = ann.priority();
      }
      if (ann.chainBreak() && ann.priority() < upper) {
        upper = ann.priority();
      }
    }

    List<T> chain = new ArrayList<>();
    if (lower > upper) {
      return new AssemblerChain<>(chain);
    }
    for (Class<?> candidate : matching) {
      int priority = annotation(candidate).priority();
      if (priority >= lower && priority <= upper) {
        chain.add(clazz.cast(instantiate(candidate)));
      }
    }
    return new AssemblerChain<>(chain);
  }

  private static boolean matches(Class<?> candidate, String key, Class<?> clazz) {
    AssemblerBinding annotation = candidate.getAnnotation(AssemblerBinding.class);
    return annotation != null
        && annotation.value().equals(key)
        && clazz.isAssignableFrom(candidate);
  }

  private static AssemblerBinding annotation(Class<?> candidate) {
    return candidate.getAnnotation(AssemblerBinding.class);
  }

  private static @Nullable String currentObjectType(Resolver context) {
    Resolver root = context.root();
    return (root instanceof EntityResolver entity) ? entity.entity().type() : null;
  }

  private boolean isEligible(AssemblerBinding ann, @Nullable String objectType, Resolver context) {
    return matchesObjectType(ann, objectType) && appliesInContext(ann, context);
  }

  private static boolean matchesObjectType(AssemblerBinding ann, @Nullable String objectType) {
    return ann.objectTypes().length == 0
        || (objectType != null && Arrays.asList(ann.objectTypes()).contains(objectType));
  }

  private boolean appliesInContext(AssemblerBinding ann, Resolver context) {
    Class<? extends AssemblerCondition> conditionType = ann.condition();
    // Short-circuits on the default condition, so Always is never instantiated.
    return AssemblerCondition.Always.class.equals(conditionType)
        || ((AssemblerCondition) instantiate(conditionType)).appliesTo(context);
  }

  /**
   * Builds a discovered class through the injector, exactly as production builds it: from its
   * {@code @Inject} constructor, with every collaborator resolved recursively.
   *
   * <p>Guice reports a missing binding against the type it could not reach, which is often several
   * constructors deep; the class actually asked for is added here, because that is the one whose
   * scenario failed.
   */
  private Object instantiate(Class<?> impl) {
    try {
      return this.injector.getInstance(impl);
    } catch (ConfigurationException | ProvisionException e) {
      throw new IllegalStateException("Cannot instantiate " + impl.getName(), e);
    }
  }
}
