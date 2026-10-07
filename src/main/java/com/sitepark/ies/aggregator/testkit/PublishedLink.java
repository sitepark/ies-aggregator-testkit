package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.resolver.Resolver;
import org.jspecify.annotations.Nullable;

/**
 * An external link an aggregator reported to the publisher during a scenario, see {@link
 * ScenarioContext#publishedLinks()}.
 *
 * @param scope the node the link was read from, or {@code null} if it belongs to the resource as a
 *     whole
 * @param url the reported URL
 */
public record PublishedLink(@Nullable Resolver scope, String url) {}
