package com.sitepark.ies.aggregator.testkit.chainfixture;

import com.sitepark.ies.aggregator.AssemblerBinding;

@AssemblerBinding("chain.cut")
public final class CutBottom implements ChainProbe {
  @Override
  public String name() {
    return "bottom";
  }
}
