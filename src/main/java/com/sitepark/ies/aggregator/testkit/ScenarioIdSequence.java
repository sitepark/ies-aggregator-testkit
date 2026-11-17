package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.port.IdSequence;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * {@link IdSequence} of a scenario: one scenario is one resource, so the counters live as long as
 * the {@link ScenarioContext} and run across every section aggregated on it, one counter per name,
 * as SiteKit's counters run across a page.
 *
 * <p>A scenario that shows a section in the middle of a page says how many ids the sections before
 * it already drew: {@code "idSequence": {"text": 3}} makes the next {@code text} id {@code
 * text-4}. A name the scenario does not mention starts at 1.
 */
final class ScenarioIdSequence implements IdSequence {

  private final Map<String, Integer> counters = new HashMap<>();

  /**
   * @param raw the scenario's {@code idSequence} block, a map {@code name -> ids already drawn}, or
   *     {@code null} if it has none
   */
  ScenarioIdSequence(@Nullable Object raw) {
    if (raw == null) {
      return;
    }
    if (!(raw instanceof Map<?, ?> drawn)) {
      throw new IllegalArgumentException("'idSequence' must be an object: " + raw);
    }
    for (Map.Entry<?, ?> entry : drawn.entrySet()) {
      if (!(entry.getValue() instanceof Integer count) || count < 0) {
        throw new IllegalArgumentException(
            "'idSequence."
                + entry.getKey()
                + "' must be a count of 0 or more: "
                + entry.getValue());
      }
      this.counters.put(String.valueOf(entry.getKey()), count);
    }
  }

  @Override
  public String next(String name) {
    int next = this.counters.merge(name, 1, Integer::sum);
    return name + "-" + next;
  }
}
