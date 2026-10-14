package com.sitepark.ies.aggregator.testkit;

import com.google.inject.Module;
import com.sitepark.ies.extension.api.ConfigurationContext;
import com.sitepark.ies.extension.api.Extension;
import com.sitepark.ies.extension.api.annotations.ConfigureFactory;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Guice modules the {@link Extension}s under the scanned packages contribute, collected the way
 * the production {@code ExtensionFactory} collects them: every public static method annotated
 * {@link ConfigureFactory} is called with a {@link ConfigurationContext}, and what it adds there is
 * installed next to the ports.
 *
 * <p>That is how an aggregator JAR binds a port the host does not provide — and how a module built
 * on it replaces that binding, through an {@code OptionalBinder}. Installing the same modules here
 * keeps the harness wired like production: an assembler that needs such a port finds the binding the
 * JAR ships, not one the harness invents. A project's own test sources may declare an extension too,
 * to put a scenario-backed implementation in place of the shipped default.
 */
final class ExtensionModules {

  /** Cached per base-package set — the classpath does not change between tests within a JVM. */
  private static final Map<Set<String>, List<Class<Extension>>> EXTENSION_CLASSES_CACHE =
      new ConcurrentHashMap<>();

  private ExtensionModules() {}

  /**
   * Returns the modules the {@link ConfigureFactory} methods of the extensions contribute, in
   * discovery order.
   *
   * @param packages the base packages to scan for {@link Extension} classes
   */
  static List<Module> of(Collection<String> packages) {
    ConfigurationContext context = new ConfigurationContext();
    for (Class<Extension> extension : extensionClasses(packages)) {
      for (Method method : extension.getMethods()) {
        if (method.isAnnotationPresent(ConfigureFactory.class)) {
          configure(extension, method, context);
        }
      }
    }
    return List.copyOf(context.getInjectionModuleList());
  }

  private static List<Class<Extension>> extensionClasses(Collection<String> packages) {
    return EXTENSION_CLASSES_CACHE.computeIfAbsent(
        new TreeSet<>(packages), ExtensionModules::discover);
  }

  private static List<Class<Extension>> discover(Set<String> packages) {
    try (ScanResult scan =
        new ClassGraph().enableClassInfo().acceptPackages(packages.toArray(String[]::new)).scan()) {
      return scan.getClassesImplementing(Extension.class)
          .filter(info -> !info.isAbstract() && !info.isInterface())
          .loadClasses(Extension.class);
    }
  }

  /** Production refuses a non-static configure method, and so does the harness. */
  private static void configure(
      Class<Extension> extension, Method method, ConfigurationContext context) {
    if (!Modifier.isStatic(method.getModifiers())) {
      throw new IllegalStateException(
          "The @ConfigureFactory method " + method + " of " + extension + " must be static");
    }
    try {
      method.invoke(null, context);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException(
          "The @ConfigureFactory method " + method + " of " + extension + " failed", e);
    }
  }
}
