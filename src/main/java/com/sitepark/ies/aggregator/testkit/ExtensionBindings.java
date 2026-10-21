package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.extension.api.ConfigurationContext;
import com.sitepark.ies.extension.api.ConfigurationContext.Binding;
import com.sitepark.ies.extension.api.Extension;
import com.sitepark.ies.extension.api.annotations.ConfigureFactory;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.springframework.context.support.GenericApplicationContext;

/**
 * The components and bindings the {@link Extension}s under the scanned packages register, collected
 * the way the production {@code ExtensionFactory} collects them: every public static method
 * annotated {@link ConfigureFactory} is called with a {@link ConfigurationContext}, and what it
 * registers there is declared next to the ports.
 *
 * <p>That is how an aggregator JAR binds a port the host does not provide ({@link
 * ConfigurationContext#bindDefault}) — and how a module built on it replaces that binding ({@link
 * ConfigurationContext#bind}). Declaring the same bindings here keeps the harness wired like
 * production: an assembler that needs such a port finds the binding the JAR ships, not one the
 * harness invents. A project's own test sources may declare an extension too, to put a
 * scenario-backed implementation in place of the shipped default.
 *
 * <p>The mappers of an extension ({@link ConfigurationContext#addMappers}) are not declared: a
 * scenario has no database.
 */
final class ExtensionBindings {

  /** Cached per base-package set — the classpath does not change between tests within a JVM. */
  private static final Map<Set<String>, List<Class<Extension>>> EXTENSION_CLASSES_CACHE =
      new ConcurrentHashMap<>();

  private ExtensionBindings() {}

  /**
   * Returns the registration of what the {@link ConfigureFactory} methods of the extensions
   * contribute.
   *
   * @param packages the base packages to scan for {@link Extension} classes
   */
  static Consumer<GenericApplicationContext> of(Collection<String> packages) {
    List<ConfigurationContext> configurations =
        extensionClasses(packages).stream().map(ExtensionBindings::configurationOf).toList();
    Map<Class<?>, Binding> bindings = bindings(configurations);
    return context -> {
      configurations.stream()
          .flatMap(configuration -> configuration.getComponents().stream())
          .filter(component -> !context.containsBeanDefinition(component.getName()))
          .forEach(
              component ->
                  context.registerBeanDefinition(
                      component.getName(), ScenarioContainer.definitionOf(component)));
      for (Binding binding : bindings.values()) {
        context.registerBeanDefinition(
            binding.type().getName(), ScenarioContainer.definitionOf(binding.implementation()));
      }
    };
  }

  private static ConfigurationContext configurationOf(Class<Extension> extension) {
    ConfigurationContext configuration = new ConfigurationContext();
    for (Method method : extension.getMethods()) {
      if (method.isAnnotationPresent(ConfigureFactory.class)) {
        configure(extension, method, configuration);
      }
    }
    return configuration;
  }

  /**
   * A binding replaces a default binding of the same type; two bindings of the same type are an
   * error, as two default bindings are — in production as well as here.
   */
  private static Map<Class<?>, Binding> bindings(List<ConfigurationContext> configurations) {
    Map<Class<?>, Binding> bindings = new LinkedHashMap<>();
    for (ConfigurationContext configuration : configurations) {
      for (Binding binding : configuration.getBindings()) {
        Binding existing = bindings.get(binding.type());
        if (existing == null || (existing.isDefault() && !binding.isDefault())) {
          bindings.put(binding.type(), binding);
        } else if (existing.isDefault() == binding.isDefault()) {
          throw new IllegalStateException(
              (binding.isDefault() ? "default binding" : "binding")
                  + " of "
                  + binding.type().getName()
                  + " exists twice: "
                  + existing.implementation().getName()
                  + ", "
                  + binding.implementation().getName());
        }
      }
    }
    return bindings;
  }

  private static List<Class<Extension>> extensionClasses(Collection<String> packages) {
    return EXTENSION_CLASSES_CACHE.computeIfAbsent(
        new TreeSet<>(packages), ExtensionBindings::discover);
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
