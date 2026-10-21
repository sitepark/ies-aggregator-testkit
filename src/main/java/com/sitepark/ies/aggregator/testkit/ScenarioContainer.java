package com.sitepark.ies.aggregator.testkit;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

/**
 * The container a scenario runs in, built like the production context of the extensions: the
 * registrations declare the ports and the bindings of the extensions, every concrete class beyond
 * them is created just in time from its {@code @Inject} constructor.
 */
final class ScenarioContainer {

  private ScenarioContainer() {}

  /** Starts a container with the beans the registrations declare. */
  static ApplicationContext start(List<Consumer<GenericApplicationContext>> registrations) {
    JustInTimeBeanFactory beanFactory = new JustInTimeBeanFactory();
    // an extension that binds a port the harness provides is an error, not a silent replacement
    beanFactory.setAllowBeanDefinitionOverriding(false);
    AnnotationConfigApplicationContext context =
        new AnnotationConfigApplicationContext(beanFactory);
    context.setId("ies-aggregator-testkit");
    registrations.forEach(registration -> registration.accept(context));
    context.refresh();
    return context;
  }

  /**
   * A bean definition the way production declares a registered class: a singleton if the class is
   * annotated with {@code @Singleton}, otherwise every injection gets a new instance.
   */
  static RootBeanDefinition definitionOf(Class<?> type) {
    RootBeanDefinition definition = new RootBeanDefinition(type);
    definition.setScope(
        type.isAnnotationPresent(Singleton.class)
            ? BeanDefinition.SCOPE_SINGLETON
            : BeanDefinition.SCOPE_PROTOTYPE);
    definition.setLazyInit(true);
    return definition;
  }

  /**
   * Creates a class that is asked for, but is no bean, as the production context of the extensions
   * does: with a constructor annotated with {@code @Inject} or a constructor without parameters that
   * is not private. Interfaces, abstract classes and the classes of the JDK need a bean.
   */
  private static final class JustInTimeBeanFactory extends DefaultListableBeanFactory {

    private static final long serialVersionUID = 1L;

    private final transient ReentrantLock lock = new ReentrantLock();

    @Override
    public @Nullable Object doResolveDependency(
        DependencyDescriptor descriptor,
        @Nullable String beanName,
        @Nullable Set<String> autowiredBeanNames,
        @Nullable TypeConverter typeConverter)
        throws BeansException {
      this.registerJustInTime(descriptor.getDependencyType());
      return super.doResolveDependency(descriptor, beanName, autowiredBeanNames, typeConverter);
    }

    @Override
    public <T> T getBean(Class<T> requiredType) throws BeansException {
      this.registerJustInTime(requiredType);
      return super.getBean(requiredType);
    }

    private void registerJustInTime(Class<?> type) {
      if (!isCreatableJustInTime(type)) {
        return;
      }
      this.lock.lock();
      try {
        if (BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this, type, true, false).length
            == 0) {
          this.registerBeanDefinition(type.getName(), definitionOf(type));
        }
      } finally {
        this.lock.unlock();
      }
    }

    private static boolean isCreatableJustInTime(Class<?> type) {
      if (type.isInterface()
          || type.isPrimitive()
          || type.isArray()
          || type.isEnum()
          || type.isRecord()
          || type.isAnnotation()
          || Modifier.isAbstract(type.getModifiers())
          || type.getName().startsWith("java.")
          || type.getName().startsWith("jakarta.")
          || type.getName().startsWith("org.springframework.")
          || Collection.class.isAssignableFrom(type)
          || Map.class.isAssignableFrom(type)
          || (type.isMemberClass() && !Modifier.isStatic(type.getModifiers()))) {
        return false;
      }
      for (Constructor<?> constructor : type.getDeclaredConstructors()) {
        if (constructor.isAnnotationPresent(Inject.class)
            || (constructor.getParameterCount() == 0
                && !Modifier.isPrivate(constructor.getModifiers()))) {
          return true;
        }
      }
      return false;
    }
  }
}
