package com.sitepark.ies.aggregator.testkit.chainfixture;

import com.sitepark.ies.aggregator.AssemblerBinding;

@AssemblerBinding(value = "chain.empty", priority = 3, chainBreak = true)
public final class EmptyBreak implements ChainProbe {
  @Override
  public String name() {
    return "brk";
  }
}
