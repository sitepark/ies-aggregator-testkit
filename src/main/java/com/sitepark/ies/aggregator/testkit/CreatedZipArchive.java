package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.value.media.ZipArchive;
import java.util.List;

/**
 * A zip archive an aggregator had the publisher write during a scenario, see {@link
 * ScenarioContext#zipArchives()}.
 *
 * @param archive what the aggregator was told: file name, URL and size
 * @param entries the file names of the entries, in the order they entered the archive
 */
public record CreatedZipArchive(ZipArchive archive, List<String> entries) {

  /** Copies the entries so that the record cannot change afterwards. */
  public CreatedZipArchive {
    entries = List.copyOf(entries);
  }
}
