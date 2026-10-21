package com.sitepark.ies.aggregator.testkit;

import java.util.List;

/**
 * The base packages {@link ScenarioAssemblerFactory} scans for {@code @AssemblerBinding} classes.
 *
 * <p>A type of its own rather than an annotated {@code List<String>}, because the container needs an
 * unambiguous key and a qualified bean of a generic collection reads far worse at both ends.
 *
 * @param packages the base packages to scan, outermost first
 */
record AssemblerPackages(List<String> packages) {

  AssemblerPackages {
    packages = List.copyOf(packages);
  }

  static AssemblerPackages of(String... packages) {
    return new AssemblerPackages(List.of(packages));
  }
}
