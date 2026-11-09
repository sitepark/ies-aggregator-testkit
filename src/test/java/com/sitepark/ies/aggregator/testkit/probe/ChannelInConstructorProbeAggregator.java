package com.sitepark.ies.aggregator.testkit.probe;

import com.sitepark.ies.aggregator.Aggregator;
import com.sitepark.ies.aggregator.output.OutputNode;
import com.sitepark.ies.aggregator.port.Channel;
import com.sitepark.ies.aggregator.resolver.Resolver;
import jakarta.inject.Inject;

/** Injects the channel itself - fine inside the aggregation scope, an error where it is built. */
public final class ChannelInConstructorProbeAggregator implements Aggregator {

  private final Channel channel;

  @Inject
  ChannelInConstructorProbeAggregator(Channel channel) {
    this.channel = channel;
  }

  @Override
  public void aggregate(Resolver source, OutputNode output) {
    output.put("channel", this.channel.name());
  }
}
