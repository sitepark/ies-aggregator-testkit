package com.sitepark.ies.aggregator.testkit.chainfixture;

import com.sitepark.ies.aggregator.AssemblerBinding;

/** Object-type-restricted probe: applies only to the {@code news} object type. */
@AssemblerBinding(
    value = "chain.otype",
    priority = 10,
    objectTypes = {"news"})
public final class OtypeNews implements ChainProbe {
  @Override
  public String name() {
    return "news";
  }
}
