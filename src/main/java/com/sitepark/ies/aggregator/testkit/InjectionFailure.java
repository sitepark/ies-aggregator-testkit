package com.sitepark.ies.aggregator.testkit;

import org.springframework.beans.BeansException;
import org.springframework.core.NestedExceptionUtils;

/**
 * Turns a failure of the container into an exception that names what the scenario was building.
 *
 * <p>The container reports a failure against the innermost bean it could not create, often several
 * constructors deep, wrapped in one {@link BeansException} per level. The message of the outermost
 * one is a chain of "Error creating bean ..." sentences; what a scenario author needs is the class
 * that was asked for and the root cause, so the failure is described from those, and the root cause is
 * kept as the cause.
 */
final class InjectionFailure {

  private InjectionFailure() {}

  /** Describes a failed provisioning; {@code what} opens the message. */
  static IllegalStateException of(String what, BeansException failure) {
    Throwable root = NestedExceptionUtils.getMostSpecificCause(failure);
    return new IllegalStateException(
        what + ": " + root.getClass().getSimpleName() + ": " + root.getMessage(), root);
  }
}
