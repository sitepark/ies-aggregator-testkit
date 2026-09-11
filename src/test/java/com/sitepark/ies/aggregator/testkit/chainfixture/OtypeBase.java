package com.sitepark.ies.aggregator.testkit.chainfixture;

import com.sitepark.ies.aggregator.AssemblerBinding;

/** Wildcard probe (no {@code objectTypes}) for the object-type selection tests. */
@AssemblerBinding("chain.otype")
public final class OtypeBase implements ChainProbe {
  @Override
  public String name() {
    return "base";
  }
}
