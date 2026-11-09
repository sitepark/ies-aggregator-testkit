package com.sitepark.ies.aggregator.testkit;

/**
 * A module dependency an aggregator reported to the publisher during a scenario, see {@link
 * ScenarioContext#moduleDependencies()}.
 *
 * @param moduleId the id of the installed module
 * @param version the version the scenario's {@code modules} block names for it, which the publisher
 *     would have stored with the dependency
 */
public record ModuleDependency(String moduleId, String version) {}
