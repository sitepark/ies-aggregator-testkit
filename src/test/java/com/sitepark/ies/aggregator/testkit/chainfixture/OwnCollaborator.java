package com.sitepark.ies.aggregator.testkit.chainfixture;

import com.sitepark.ies.aggregator.AssemblerBinding;
import jakarta.inject.Inject;

/**
 * A probe assembler with a collaborator of its own ({@link ProbeGreeting}), which in turn depends on
 * a port.
 *
 * <p>This is the case a custom-extension project brings: an assembler whose collaborator the testkit
 * has never heard of. Nothing registers {@code ProbeGreeting} anywhere — if this probe can be
 * created, the harness builds foreign object graphs on its own, and a project like
 * {@code citygov-aggregator} can test its assemblers without changing the testkit.
 */
@AssemblerBinding("chain.collaborator")
public final class OwnCollaborator implements ChainProbe {

  private final ProbeGreeting greeting;

  @Inject
  OwnCollaborator(ProbeGreeting greeting) {
    this.greeting = greeting;
  }

  @Override
  public String name() {
    return this.greeting.greet();
  }
}
