package com.sitepark.ies.aggregator.testkit.chainfixture;

import com.sitepark.ies.aggregator.port.Channel;
import jakarta.inject.Inject;

/**
 * A collaborator of a probe assembler, standing in for the ones a custom-extension project brings
 * along: its own class, known neither to {@code ScenarioModule} nor to
 * {@code ScenarioAssemblerFactory}, reachable only because the injector builds it from this
 * constructor and resolves the port it asks for.
 */
public final class ProbeGreeting {

  private final Channel channel;

  @Inject
  ProbeGreeting(Channel channel) {
    this.channel = channel;
  }

  /** Proves the injected port arrived, not just the collaborator itself. */
  String greet() {
    return this.channel == null ? "no channel" : "channel";
  }
}
