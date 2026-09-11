package com.sitepark.ies.aggregator.testkit.chainfixture;

/**
 * Minimal assembler type used only to test chain assembly (ordering and {@code chainRoot}/{@code
 * chainBreak} pruning) in {@code ScenarioAssemblerFactory}. Its implementations carry their own
 * {@code @AssemblerBinding} keys, so they never interfere with the production scenario tests.
 */
public interface ChainProbe {
  String name();
}
