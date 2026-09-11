package com.sitepark.ies.aggregator.testkit.chainfixture;

import com.sitepark.ies.aggregator.AssemblerBinding;

/** Unconditional probe for the custom-condition selection tests. */
@AssemblerBinding("chain.cond")
public final class CondBase implements ChainProbe {
  @Override
  public String name() {
    return "base";
  }
}
