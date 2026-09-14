package com.sitepark.ies.aggregator.testkit.probe;

import com.sitepark.ies.aggregator.Options;
import org.jspecify.annotations.Nullable;

/**
 * Options of {@link ProbeAggregator}: a greeting the aggregator writes beside the headline.
 *
 * @param greeting what to write, or {@code null} when the scenario configures nothing
 */
public record ProbeOptions(@Nullable String greeting) implements Options {}
