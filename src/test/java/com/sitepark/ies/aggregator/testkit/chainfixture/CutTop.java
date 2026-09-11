package com.sitepark.ies.aggregator.testkit.chainfixture;

import com.sitepark.ies.aggregator.AssemblerBinding;

@AssemblerBinding(value = "chain.cut", priority = 10)
public final class CutTop implements ChainProbe {
  @Override
  public String name() {
    return "top";
  }
}
