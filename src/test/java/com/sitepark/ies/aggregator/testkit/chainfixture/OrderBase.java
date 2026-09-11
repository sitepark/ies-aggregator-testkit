package com.sitepark.ies.aggregator.testkit.chainfixture;

import com.sitepark.ies.aggregator.AssemblerBinding;

@AssemblerBinding("chain.order")
public final class OrderBase implements ChainProbe {
  @Override
  public String name() {
    return "base";
  }
}
