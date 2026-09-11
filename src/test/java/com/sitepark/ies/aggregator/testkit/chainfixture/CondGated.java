package com.sitepark.ies.aggregator.testkit.chainfixture;

import com.sitepark.ies.aggregator.AssemblerBinding;

/** Probe gated by a custom {@link NewsOnlyCondition}: applies only in a {@code news} scope. */
@AssemblerBinding(value = "chain.cond", priority = 10, condition = NewsOnlyCondition.class)
public final class CondGated implements ChainProbe {
  @Override
  public String name() {
    return "gated";
  }
}
