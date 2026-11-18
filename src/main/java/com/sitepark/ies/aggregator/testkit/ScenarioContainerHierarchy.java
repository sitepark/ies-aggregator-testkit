package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.container.ContainerEntry;
import com.sitepark.ies.aggregator.container.ContainerNode;
import com.sitepark.ies.aggregator.container.ContainerSection;
import com.sitepark.ies.aggregator.container.HierarchyRule;
import com.sitepark.ies.aggregator.port.ContainerHierarchy;
import com.sitepark.ies.aggregator.resolver.Resolver;
import com.sitepark.ies.aggregator.resolver.ResolverPath;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * {@link ContainerHierarchy} of a scenario: the tree is not derived, the scenario states it. There
 * is no copy of the arranger here - which entry opens which section is the platform's business, and
 * a scenario that wants to see a container's output says what the platform answered.
 *
 * <p>The scenario's {@code containers} block maps the address of a container's list to its tree:
 *
 * <pre>
 * "containers": {
 *   "sp_main_iterate": [
 *     {"entry": "2001", "index": 0, "rule": {"newSection": true, "sectionLevel": 1}},
 *     {"section": {"outputType": "section", "implicit": false, "ordinal": 1,
 *                  "opener": {"entry": "2002", "index": 1, "rule": {"newSection": true}},
 *                  "children": [{"entry": "2003", "index": 2, "rule": {}}]}}
 *   ],
 *   "2002/sp_content_iterate": [ ... ]
 * }
 * </pre>
 *
 * <p>The address is the list field alone when the container lies in the source of the scenario, and
 * {@code <entry id>/<list field>} when it lies in another entry (a container nested in a section,
 * whose data is that entry). An entry names the key of a repository entry; it reaches the
 * aggregator as the resolver on that entry, as the item of a list does. {@code rule} holds the
 * fields of the {@code aggregator} configuration of the entry's section type, read by the very
 * {@link HierarchyRule#of} the platform uses; without it the entry's section type is not configured
 * for the container.
 *
 * <p>No more generous than production, where nothing answers for a container it does not know:
 *
 * <ul>
 *   <li>a scope or list field the scenario gives no tree for has no entries - the tree of the source
 *       is never handed out for another scope;
 *   <li>a scope that has no {@code id} cannot be addressed and has no entries either;
 *   <li>the tree is taken as stated - nothing is dropped, regrouped or numbered here, and a
 *       malformed tree fails the scenario instead of being repaired.
 * </ul>
 */
final class ScenarioContainerHierarchy implements ContainerHierarchy {

  private static final Set<String> ENTRY_KEYS = Set.of("entry", "index", "rule");
  private static final Set<String> SECTION_KEYS =
      Set.of("outputType", "implicit", "ordinal", "opener", "children");

  private final String sourceId;
  private final Map<String, List<ContainerNode<Resolver>>> trees = new LinkedHashMap<>();

  /**
   * @param repository the entries the tree's entries are resolved from
   * @param sourceId the id of the entry a plain list field address belongs to
   * @param raw the scenario's {@code containers} block, or {@code null} if it has none
   */
  ScenarioContainerHierarchy(Repository repository, String sourceId, @Nullable Object raw) {
    this.sourceId = sourceId;
    if (raw == null) {
      return;
    }
    for (Map.Entry<String, Object> tree : object(raw, "containers").entrySet()) {
      this.trees.put(tree.getKey(), nodes(repository, tree.getValue(), tree.getKey()));
    }
  }

  @Override
  public List<ContainerNode<Resolver>> of(Resolver scope, String iteratorFieldName) {
    if (!(scope instanceof RepositoryResolver resolver)) {
      return List.of();
    }
    String id = resolver.entryId();
    if (id.isEmpty()) {
      return List.of();
    }
    String address = id.equals(this.sourceId) ? iteratorFieldName : id + "/" + iteratorFieldName;
    return this.trees.getOrDefault(address, List.of());
  }

  private static List<ContainerNode<Resolver>> nodes(
      Repository repository, Object raw, String where) {
    if (!(raw instanceof List<?> list)) {
      throw new IllegalArgumentException("Container " + where + " must be a list of nodes: " + raw);
    }
    List<ContainerNode<Resolver>> nodes = new ArrayList<>();
    for (Object node : list) {
      nodes.add(node(repository, node, where));
    }
    return List.copyOf(nodes);
  }

  private static ContainerNode<Resolver> node(Repository repository, Object raw, String where) {
    Map<String, Object> node = object(raw, where);
    if (node.containsKey("section")) {
      if (node.size() != 1) {
        throw new IllegalArgumentException(
            "A section node holds nothing beside 'section' (" + where + "): " + node.keySet());
      }
      return section(repository, object(node.get("section"), where), where);
    }
    return entry(repository, node, where);
  }

  private static ContainerSection<Resolver> section(
      Repository repository, Map<String, Object> section, String where) {
    rejectUnknown(section, SECTION_KEYS, "section", where);
    Object opener = section.get("opener");
    return new ContainerSection<>(
        text(section, "outputType", where),
        Boolean.TRUE.equals(section.get("implicit")),
        number(section, "ordinal", where),
        opener == null ? null : entry(repository, object(opener, where), where),
        section.get("children") == null
            ? List.of()
            : nodes(repository, section.get("children"), where));
  }

  private static ContainerEntry<Resolver> entry(
      Repository repository, Map<String, Object> entry, String where) {
    rejectUnknown(entry, ENTRY_KEYS, "entry", where);
    String key = text(entry, "entry", where);
    Resolver resolver = ResolverPath.createRoot(path -> repository.resolver(path, key));
    Object rule = entry.get("rule");
    return new ContainerEntry<>(
        resolver,
        number(entry, "index", where),
        rule == null ? null : HierarchyRule.of(object(rule, where)));
  }

  private static void rejectUnknown(
      Map<String, Object> node, Set<String> known, String kind, String where) {
    for (String key : node.keySet()) {
      if (!known.contains(key)) {
        throw new IllegalArgumentException(
            "Unknown key '" + key + "' in a " + kind + " of container " + where);
      }
    }
  }

  private static String text(Map<String, Object> node, String key, String where) {
    if (node.get(key) instanceof String text && !text.isBlank()) {
      return text;
    }
    throw new IllegalArgumentException(
        "'" + key + "' must be a non-blank string in container " + where + ": " + node);
  }

  private static int number(Map<String, Object> node, String key, String where) {
    if (node.get(key) instanceof Number number) {
      return number.intValue();
    }
    throw new IllegalArgumentException(
        "'" + key + "' must be a number in container " + where + ": " + node);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> object(@Nullable Object raw, String where) {
    if (raw instanceof Map<?, ?> map) {
      return (Map<String, Object>) map;
    }
    throw new IllegalArgumentException("Expected an object in container " + where + ": " + raw);
  }
}
