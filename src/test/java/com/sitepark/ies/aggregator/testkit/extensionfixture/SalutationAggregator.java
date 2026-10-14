package com.sitepark.ies.aggregator.testkit.extensionfixture;

import com.sitepark.ies.aggregator.Aggregator;
import com.sitepark.ies.aggregator.output.OutputNode;
import com.sitepark.ies.aggregator.resolver.Resolver;
import jakarta.inject.Inject;

/** An aggregator that needs the {@link Salutation} port, as an assembler of the JAR would. */
public final class SalutationAggregator implements Aggregator {

  private final Salutation salutation;

  @Inject
  SalutationAggregator(Salutation salutation) {
    this.salutation = salutation;
  }

  /** What the port it was built with answers. */
  public String salutation() {
    return this.salutation.text();
  }

  @Override
  public void aggregate(Resolver source, OutputNode output) {
    output.put("salutation", this.salutation.text());
  }
}
