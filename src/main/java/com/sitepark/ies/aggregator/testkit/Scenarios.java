package com.sitepark.ies.aggregator.testkit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Discovers JSON test scenarios on the classpath so section-type aggregator tests can run them as
 * parameterized cases instead of one method per scenario.
 *
 * <p>Within a directory, every {@code <name>.json} that is not a {@code <name>.expected.json} golden
 * file becomes one {@link Scenario}, paired with its expected file. New scenarios are picked up
 * simply by dropping the two files into the directory — no test code change required.
 */
public final class Scenarios {

  private Scenarios() {}

  private static final String INPUT_SUFFIX = ".json";
  private static final String EXPECTED_SUFFIX = ".expected.json";

  /**
   * Returns all scenarios found directly under the given classpath directories, sorted by name
   * within each directory.
   *
   * <p>Several directories are for a subject that is covered from more than one angle - one
   * aggregator, two techniques, say - and whose cases would be misleading if they were mixed into
   * one directory.
   *
   * @param directories the classpath directories to scan (e.g. {@code
   *     "scenarios/content-artdirection"})
   * @throws IllegalStateException if a directory does not exist on the classpath
   */
  public static List<Scenario> discover(String... directories) {
    if (directories.length == 1) {
      return discoverOne(directories[0]);
    }
    return Arrays.stream(directories).map(Scenarios::discoverOne).flatMap(List::stream).toList();
  }

  private static List<Scenario> discoverOne(String directory) {
    Path dir = locate(directory);
    try (Stream<Path> files = Files.list(dir)) {
      return files
          .map(path -> path.getFileName().toString())
          .filter(name -> name.endsWith(INPUT_SUFFIX) && !name.endsWith(EXPECTED_SUFFIX))
          .sorted()
          .map(fileName -> toScenario(directory, fileName))
          .toList();
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot list scenarios in " + directory, e);
    }
  }

  private static Scenario toScenario(String directory, String fileName) {
    String name = fileName.substring(0, fileName.length() - INPUT_SUFFIX.length());
    return new Scenario(name, directory + "/" + fileName, directory + "/" + name + EXPECTED_SUFFIX);
  }

  // Scenarios live next to the tests of the project using the harness, on the same classpath as
  // this class; no container class loader is in play.
  @SuppressWarnings("PMD.UseProperClassLoader")
  private static Path locate(String directory) {
    URL url = Scenarios.class.getClassLoader().getResource(directory);
    if (url == null) {
      throw new IllegalStateException("Scenario directory not found on classpath: " + directory);
    }
    try {
      return Path.of(url.toURI());
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Invalid scenario directory URL: " + url, e);
    }
  }
}
