package com.sitepark.ies.aggregator.testkit.probe;

import com.sitepark.ies.aggregator.Aggregator;
import com.sitepark.ies.aggregator.SubAggregatorAware;
import com.sitepark.ies.aggregator.SubSectionType;
import com.sitepark.ies.aggregator.SubSectionTypesAware;
import com.sitepark.ies.aggregator.output.OutputList;
import com.sitepark.ies.aggregator.output.OutputListItem;
import com.sitepark.ies.aggregator.output.OutputNode;
import com.sitepark.ies.aggregator.resolver.Resolver;
import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A container: it gets its children and their configuration as production hands them in, and
 * writes one entry per offered section type into the node {@code container} of the root - never
 * into its own node, as a container does under the SPML page.
 */
// a probe: it exposes what it was handed, so the tests can look at it
@SuppressWarnings("PMD.DataClass")
public final class PlacingProbeAggregator
    implements Aggregator, SubAggregatorAware, SubSectionTypesAware {

  private final Map<String, Aggregator> subAggregators = new LinkedHashMap<>();
  private final Map<String, SubSectionType> subSectionTypes = new LinkedHashMap<>();

  @Inject
  PlacingProbeAggregator() {}

  @Override
  public void setSubAggregators(Map<String, Aggregator> subAggregators) {
    this.subAggregators.putAll(subAggregators);
  }

  @Override
  public void setSubSectionTypes(Map<String, SubSectionType> subSectionTypes) {
    this.subSectionTypes.putAll(subSectionTypes);
  }

  /** The children as they were handed in. */
  public Map<String, Aggregator> subAggregators() {
    return this.subAggregators;
  }

  /** The configuration of the children as it was handed in. */
  public Map<String, SubSectionType> subSectionTypes() {
    return this.subSectionTypes;
  }

  @Override
  public void aggregate(Resolver source, OutputNode output) {
    OutputList items = output.root().node("container").nodeList("items");
    for (Map.Entry<String, SubSectionType> offered : this.subSectionTypes.entrySet()) {
      Aggregator child = this.subAggregators.get(offered.getKey());
      if (child == null || !offered.getValue().enabledFor(Optional.empty())) {
        continue;
      }
      OutputListItem item = items.addItem();
      item.put("type", offered.getValue().componentType());
      item.put("id", offered.getKey());
      child.aggregate(source, item);
    }
  }
}
