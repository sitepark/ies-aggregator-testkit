package com.sitepark.ies.aggregator.testkit.extensionfixture.base;

import com.google.inject.multibindings.OptionalBinder;
import com.sitepark.ies.aggregator.testkit.extensionfixture.Salutation;
import com.sitepark.ies.extension.api.ConfigurationContext;
import com.sitepark.ies.extension.api.Extension;
import com.sitepark.ies.extension.api.annotations.ConfigureFactory;

/** Plays the aggregator JAR that declares {@link Salutation} and ships its default. */
public final class SalutationExtension implements Extension {

  public static final String DEFAULT = "shipped default";

  @ConfigureFactory
  public static void configureInjection(ConfigurationContext context) {
    context.addInjectionModule(
        binder ->
            OptionalBinder.newOptionalBinder(binder, Salutation.class)
                .setDefault()
                .toInstance(() -> DEFAULT));
  }

  @Override
  public void initialize() {
    // nothing to start
  }
}
