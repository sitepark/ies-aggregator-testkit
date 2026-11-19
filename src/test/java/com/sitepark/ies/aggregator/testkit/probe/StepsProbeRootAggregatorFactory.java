package com.sitepark.ies.aggregator.testkit.probe;

import com.sitepark.ies.aggregator.Aggregator;
import com.sitepark.ies.aggregator.RootAggregator;
import com.sitepark.ies.aggregator.RootAggregatorFactory;
import com.sitepark.ies.aggregator.SubAggregatorAware;
import com.sitepark.ies.aggregator.SubSectionType;
import com.sitepark.ies.aggregator.SubSectionTypesAware;
import com.sitepark.ies.aggregator.output.OutputList;
import com.sitepark.ies.aggregator.output.OutputListItem;
import java.util.List;
import java.util.Map;

/**
 * A root aggregator factory that builds the page from its object type's steps: it gets the placed
 * section types as production hands them in and writes what it got into the node {@code steps} of
 * the root - one item per placed section type, in the order it was handed, with its key, the id of
 * its configuration and whether it came with an aggregator - and then lets the Java ones run.
 */
public class StepsProbeRootAggregatorFactory
    implements RootAggregatorFactory, SubAggregatorAware, SubSectionTypesAware {

  private Map<String, Aggregator> subAggregators = Map.of();
  private Map<String, SubSectionType> subSectionTypes = Map.of();

  @Override
  public void setSubAggregators(Map<String, Aggregator> subAggregators) {
    this.subAggregators = subAggregators;
  }

  @Override
  public void setSubSectionTypes(Map<String, SubSectionType> subSectionTypes) {
    this.subSectionTypes = subSectionTypes;
  }

  @Override
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops") // one item per placed section type
  public RootAggregator create(int id) {
    Aggregator report =
        (source, output) -> {
          OutputList steps = output.nodeList("steps");
          for (Map.Entry<String, SubSectionType> placed : this.subSectionTypes.entrySet()) {
            OutputListItem item = new OutputListItem(steps);
            item.put("key", placed.getKey());
            item.put("id", placed.getValue().id());
            item.put("java", this.subAggregators.containsKey(placed.getKey()));
            steps.addItem(item);
          }
        };
    return new RootAggregator(List.of(report));
  }
}
