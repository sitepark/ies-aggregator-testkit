package com.sitepark.ies.aggregator.testkit.probe;

import com.sitepark.ies.aggregator.Aggregator;
import com.sitepark.ies.aggregator.container.ContainerEntry;
import com.sitepark.ies.aggregator.container.ContainerNode;
import com.sitepark.ies.aggregator.container.ContainerSection;
import com.sitepark.ies.aggregator.output.OutputNode;
import com.sitepark.ies.aggregator.port.ContainerHierarchy;
import com.sitepark.ies.aggregator.port.IdSequence;
import com.sitepark.ies.aggregator.resolver.Resolver;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Walks the container of {@code sp_main_iterate} and writes one line per node into {@code
 * container}, drawing the id of every section from the id sequence, as a container does.
 */
public final class ContainerProbeAggregator implements Aggregator {

  private final ContainerHierarchy hierarchy;
  private final IdSequence ids;

  @Inject
  ContainerProbeAggregator(ContainerHierarchy hierarchy, IdSequence ids) {
    this.hierarchy = hierarchy;
    this.ids = ids;
  }

  @Override
  public void aggregate(Resolver source, OutputNode output) {
    OutputNode container = output.root().node("container");
    container.put("first", this.ids.next("text"));
    AtomicInteger line = new AtomicInteger();
    this.walk(this.hierarchy.of(source, "sp_main_iterate"), container, line);
  }

  private void walk(List<ContainerNode<Resolver>> nodes, OutputNode out, AtomicInteger line) {
    for (ContainerNode<Resolver> node : nodes) {
      switch (node) {
        case ContainerEntry<Resolver> entry ->
            out.put("n" + line.getAndIncrement(), describe(entry));
        case ContainerSection<Resolver> section -> {
          String opener = section.opener() == null ? "-" : describe(section.opener());
          out.put(
              "n" + line.getAndIncrement(),
              this.ids.next("section")
                  + ":"
                  + section.outputType()
                  + ":ordinal="
                  + section.ordinal()
                  + ":implicit="
                  + section.implicit()
                  + ":opener="
                  + opener);
          this.walk(section.children(), out, line);
        }
      }
    }
  }

  private static String describe(ContainerEntry<Resolver> entry) {
    String rule =
        entry.rule() == null
            ? "unconfigured"
            : entry.rule().newSection() ? "opens" + entry.rule().sectionLevel() : "content";
    return entry.entry().value("sp_headline").asString() + "@" + entry.index() + "/" + rule;
  }
}
