package com.sitepark.ies.aggregator.testkit.chainfixture;

import com.sitepark.ies.aggregator.AssemblerBinding;

@AssemblerBinding(value = "chain.empty", priority = 5, chainRoot = true)
public final class EmptyRoot implements ChainProbe {
  @Override
  public String name() {
    return "root";
  }
}
