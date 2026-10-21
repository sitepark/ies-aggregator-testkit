package com.sitepark.ies.aggregator.testkit.extensionfixture.override;

import com.sitepark.ies.aggregator.testkit.extensionfixture.Salutation;
import com.sitepark.ies.extension.api.ConfigurationContext;
import com.sitepark.ies.extension.api.Extension;
import com.sitepark.ies.extension.api.annotations.ConfigureFactory;

/**
 * Plays a project's test extension: it replaces the shipped default with {@link
 * ScenarioSalutation}.
 */
public final class ScenarioSalutationExtension implements Extension {

  @ConfigureFactory
  public static void configureInjection(ConfigurationContext context) {
    context.bind(Salutation.class, ScenarioSalutation.class);
  }

  @Override
  public void initialize() {
    // nothing to start
  }
}
