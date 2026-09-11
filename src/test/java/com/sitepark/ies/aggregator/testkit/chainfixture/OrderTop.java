package com.sitepark.ies.aggregator.testkit.chainfixture;

import com.sitepark.ies.aggregator.AssemblerBinding;

@AssemblerBinding(value = "chain.order", priority = 10)
public final class OrderTop implements ChainProbe {
  @Override
  public String name() {
    return "top";
  }
}
