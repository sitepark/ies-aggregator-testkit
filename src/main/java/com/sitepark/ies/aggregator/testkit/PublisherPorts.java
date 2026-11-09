package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.port.ModuleDependencies;
import com.sitepark.ies.aggregator.port.PublishedLinks;
import com.sitepark.ies.aggregator.port.Regeneration;
import com.sitepark.ies.aggregator.port.ResourceState;

/**
 * The ports through which an aggregation talks to the publisher beside its output: the state shared
 * across one resource, the external links it reports, the moment it asks to be regenerated and the modules it depends
 * on.
 *
 * @param resourceState the state of the resource being aggregated
 * @param publishedLinks where external links are reported
 * @param regeneration where the moment of the next generation is reported
 * @param moduleDependencies where the modules the output depends on are reported
 */
record PublisherPorts(
    ResourceState resourceState,
    PublishedLinks publishedLinks,
    Regeneration regeneration,
    ModuleDependencies moduleDependencies) {}
