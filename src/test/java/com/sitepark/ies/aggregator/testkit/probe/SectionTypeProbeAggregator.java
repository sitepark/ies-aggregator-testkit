package com.sitepark.ies.aggregator.testkit.probe;

import com.sitepark.ies.aggregator.Aggregator;
import com.sitepark.ies.aggregator.SectionTypeAware;
import com.sitepark.ies.aggregator.output.OutputNode;
import com.sitepark.ies.aggregator.resolver.Resolver;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

/** An aggregator that wants to know the id of the section type it serves, as production tells it. */
public final class SectionTypeProbeAggregator implements Aggregator, SectionTypeAware {

  private @Nullable String sectionType;

  @Inject
  SectionTypeProbeAggregator() {}

  @Override
  public void setSectionType(String sectionTypeId) {
    this.sectionType = sectionTypeId;
  }

  /** The id as it was handed in, or {@code null} if nobody handed one in. */
  public @Nullable String sectionType() {
    return this.sectionType;
  }

  @Override
  public void aggregate(Resolver source, OutputNode output) {
    output.put("sectionType", this.sectionType);
  }
}
