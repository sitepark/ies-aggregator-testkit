package com.sitepark.ies.aggregator.testkit.chainfixture;

import com.sitepark.ies.aggregator.AssemblerBinding;

@AssemblerBinding(value = "chain.cut", priority = 5, chainRoot = true)
public final class CutRoot implements ChainProbe {
  @Override
  public String name() {
    return "root";
  }
}
