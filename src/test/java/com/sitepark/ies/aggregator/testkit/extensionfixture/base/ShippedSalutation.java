package com.sitepark.ies.aggregator.testkit.extensionfixture.base;

import com.sitepark.ies.aggregator.testkit.extensionfixture.Salutation;

/** The {@link Salutation} the aggregator JAR ships. */
public final class ShippedSalutation implements Salutation {

  @Override
  public String text() {
    return SalutationExtension.DEFAULT;
  }
}
