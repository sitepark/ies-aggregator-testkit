package com.sitepark.ies.aggregator.testkit;

/**
 * Where in a resource the harness puts what a component aggregates: the layout of the consuming
 * project, handed in by the project.
 *
 * <p>The harness knows how to run an aggregation and how to compare its result, but not what the
 * consumer's resource looks like — which area holds the content, how the root of the component tree
 * is marked, which container a section is aggregated into. Those are conventions of the project
 * being tested, so the project names them once and passes them in; the harness has no default, on
 * purpose.
 *
 * @param contentArea top-level key of the area the components are aggregated into
 * @param rootMarker {@code type} and {@code id} the root of the component tree is stamped with
 * @param containerType {@code type} of the container a section is aggregated into
 * @param containerId {@code id} of that container
 */
public record ScenarioLayout(
    String contentArea, String rootMarker, String containerType, String containerId) {}
