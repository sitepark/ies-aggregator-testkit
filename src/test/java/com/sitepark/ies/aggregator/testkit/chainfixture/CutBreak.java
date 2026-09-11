package com.sitepark.ies.aggregator.testkit.chainfixture;

import com.sitepark.ies.aggregator.AssemblerBinding;

@AssemblerBinding(value = "chain.cut", priority = 7, chainBreak = true)
public final class CutBreak implements ChainProbe {
  @Override
  public String name() {
    return "brk";
  }
}
