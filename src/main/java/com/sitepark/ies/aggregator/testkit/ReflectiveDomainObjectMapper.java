package com.sitepark.ies.aggregator.testkit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sitepark.ies.aggregator.output.DomainObjectMapper;
import com.sitepark.ies.aggregator.output.KeepEmpty;
import com.sitepark.ies.aggregator.output.OutputKeepIfEmpty;
import com.sitepark.ies.aggregator.output.OutputProperty;
import com.sitepark.ies.aggregator.output.OutputType;
import com.sitepark.ies.aggregator.output.OutputUnwrapped;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Reflective {@link DomainObjectMapper} for tests, mirroring the production {@code
 * JacksonDomainObjectMapper} without the full IES runtime.
 *
 * <p>It turns a domain object (a record or a getter-based POJO — e.g. a customer extension) into its
 * property map, mapping <b>structure</b> only:
 *
 * <ul>
 *   <li>{@link OutputProperty} renames the output key,
 *   <li>{@link OutputUnwrapped} inlines a nested object's properties flat (a {@code null} value
 *       contributes nothing; a {@link Map} value is merged directly, with {@code prefix}/{@code
 *       suffix} applied to the keys; an {@link Iterable} value inlines each element flat, so a list
 *       of extensions is rolled out as sibling entries),
 *   <li>{@link OutputKeepIfEmpty} on the container type or a property wraps that property's value in
 *       {@link KeepEmpty}, so the {@link com.sitepark.ies.aggregator.output.OutputVisitor} keeps it
 *       even when empty — the mapper's single emptiness duty. All other emptiness is decided by the
 *       visitor.
 * </ul>
 *
 * <p>Values that are not domain objects (text, uri, numbers, collections, ...) return {@code null},
 * so the visitor falls back to its native handling.
 */
// toProperties() returns null by contract (signalling "not a domain object", so the visitor falls
// back to native handling) rather than an empty map.
@SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
final class ReflectiveDomainObjectMapper implements DomainObjectMapper {

  // One KeepEmpty wrapper per property that asks for it is the mapping, not an allocation to hoist.
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Override
  public @Nullable Map<String, Object> toProperties(Object value) {
    if (value == null || !isPotentialDomainObject(value)) {
      return null;
    }

    boolean typeKeepsEmpty = value.getClass().isAnnotationPresent(OutputKeepIfEmpty.class);
    Map<String, Object> properties = new LinkedHashMap<>();
    // A type-level @OutputType is a synthetic discriminator with no backing component; inject it
    // first so it heads the output, matching the production JacksonDomainObjectMapper.
    OutputType outputType = value.getClass().getAnnotation(OutputType.class);
    if (outputType != null) {
      properties.put(outputType.key(), outputType.value());
    }
    for (Property property : properties(value.getClass())) {
      Object propertyValue = property.read(value);
      if (property.unwrapped() != null) {
        inline(properties, propertyValue, property.unwrapped());
      } else {
        boolean keep = typeKeepsEmpty || property.keepIfEmpty();
        properties.put(property.outputKey(), keep ? new KeepEmpty(propertyValue) : propertyValue);
      }
    }
    return properties.isEmpty() ? null : properties;
  }

  private void inline(
      Map<String, Object> target, @Nullable Object value, OutputUnwrapped unwrapped) {
    if (value == null) {
      return;
    }
    if (value instanceof Map<?, ?> map) {
      map.forEach(
          (key, nestedValue) ->
              target.put(
                  unwrapped.prefix() + (key == null ? "" : key.toString()) + unwrapped.suffix(),
                  nestedValue));
      return;
    }
    if (value instanceof Iterable<?> iterable) {
      for (Object element : iterable) {
        inline(target, element, unwrapped);
      }
      return;
    }
    Map<String, Object> nested = toProperties(value);
    if (nested != null) {
      nested.forEach(
          (key, nestedValue) ->
              target.put(unwrapped.prefix() + key + unwrapped.suffix(), nestedValue));
    }
  }

  /** Mirrors the production mapper: only genuine objects are introspected. */
  private static boolean isPotentialDomainObject(Object value) {
    return !(value instanceof CharSequence
        || value instanceof Number
        || value instanceof Boolean
        || value instanceof Enum<?>
        || value instanceof Map<?, ?>
        || value instanceof Iterable<?>
        || value.getClass().isArray());
  }

  private static List<Property> properties(Class<?> type) {
    List<Property> properties = new ArrayList<>();
    if (type.isRecord()) {
      for (RecordComponent component : type.getRecordComponents()) {
        if (!isIgnored(type, component.getName(), component.getAccessor())) {
          properties.add(property(type, component.getName(), component.getAccessor()));
        }
      }
    } else {
      for (Method method : type.getMethods()) {
        String name = getterProperty(method);
        if (name != null && !isIgnored(type, name, method)) {
          properties.add(property(type, name, method));
        }
      }
    }
    return properties;
  }

  private static Property property(Class<?> owner, String name, Method accessor) {
    OutputProperty renamed = annotation(owner, name, accessor, OutputProperty.class);
    String outputKey = renamed != null ? renamed.value() : name;
    OutputUnwrapped unwrapped = annotation(owner, name, accessor, OutputUnwrapped.class);
    boolean keepIfEmpty = annotation(owner, name, accessor, OutputKeepIfEmpty.class) != null;
    return new Property(outputKey, accessor, unwrapped, keepIfEmpty);
  }

  /** Mirrors the production mapper: a {@link JsonIgnore} property is excluded from the output. */
  private static boolean isIgnored(Class<?> owner, String name, Method accessor) {
    JsonIgnore ignore = annotation(owner, name, accessor, JsonIgnore.class);
    return ignore != null && ignore.value();
  }

  /** Returns the property name for a bean getter ({@code getX}/{@code isX}), or {@code null}. */
  private static @Nullable String getterProperty(Method method) {
    if (method.getParameterCount() != 0
        || method.getReturnType() == void.class
        || Modifier.isStatic(method.getModifiers())
        || method.getDeclaringClass() == Object.class) {
      return null;
    }
    String name = method.getName();
    if (name.startsWith("get") && name.length() > 3 && !"getClass".equals(name)) {
      return decapitalize(name.substring(3));
    }
    if (name.startsWith("is")
        && name.length() > 2
        && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {
      return decapitalize(name.substring(2));
    }
    return null;
  }

  private static String decapitalize(String name) {
    return Character.toLowerCase(name.charAt(0)) + name.substring(1);
  }

  /**
   * Reads the annotation from the accessor or, failing that, the backing field of the same name. A
   * component/property annotation with {@code @Target} METHOD/FIELD propagates to both, so checking
   * either is enough.
   */
  private static <A extends Annotation> @Nullable A annotation(
      Class<?> owner, String propertyName, Method accessor, Class<A> annotationType) {
    A onAccessor = accessor.getAnnotation(annotationType);
    if (onAccessor != null) {
      return onAccessor;
    }
    try {
      Field field = owner.getDeclaredField(propertyName);
      return field.getAnnotation(annotationType);
    } catch (NoSuchFieldException e) {
      return null;
    }
  }

  /** A single mapped property: its output key, value reader and structure annotations. */
  private record Property(
      String outputKey, Method accessor, @Nullable OutputUnwrapped unwrapped, boolean keepIfEmpty) {

    Object read(Object target) {
      try {
        return this.accessor.invoke(target);
      } catch (ReflectiveOperationException e) {
        throw new IllegalStateException(
            "Cannot read property " + this.outputKey + " of " + target.getClass(), e);
      }
    }
  }
}
