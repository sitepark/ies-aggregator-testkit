package com.sitepark.ies.aggregator.testkit.extensionfixture;

/**
 * A port the host does not provide: an aggregator JAR binds a default through an extension, a module
 * built on it may replace that.
 */
public interface Salutation {

  String text();
}
