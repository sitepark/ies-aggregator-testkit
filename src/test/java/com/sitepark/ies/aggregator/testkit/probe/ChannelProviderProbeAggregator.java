package com.sitepark.ies.aggregator.testkit.probe;

import com.sitepark.ies.aggregator.Aggregator;
import com.sitepark.ies.aggregator.output.OutputNode;
import com.sitepark.ies.aggregator.port.Channel;
import com.sitepark.ies.aggregator.resolver.Resolver;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/** Asks for the channel while it aggregates, which is where the scope is open. */
public final class ChannelProviderProbeAggregator implements Aggregator {

  private final Provider<Channel> channel;

  @Inject
  ChannelProviderProbeAggregator(Provider<Channel> channel) {
    this.channel = channel;
  }

  @Override
  public void aggregate(Resolver source, OutputNode output) {
    output.put("channel", this.channel.get().name());
  }
}
