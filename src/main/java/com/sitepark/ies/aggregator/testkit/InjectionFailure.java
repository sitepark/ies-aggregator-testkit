package com.sitepark.ies.aggregator.testkit;

import com.google.inject.ConfigurationException;
import com.google.inject.ProvisionException;
import com.google.inject.spi.Message;
import java.util.Collection;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * Turns a Guice failure into an exception that can be printed.
 *
 * <p>The Guice variant the harness uses ("classes") carries no ASM, and Guice formats the message of
 * a {@link ProvisionException} or {@link ConfigurationException} with the help of it: {@code
 * getMessage()} then throws {@code NoClassDefFoundError}. A test runner that prints the failure
 * crashes on it, and the failed scenario is reported as no test at all - a build that is green with
 * the failure in it. The raw {@link Message}s need no ASM, so the failure is described from those
 * and the Guice exception itself is not kept as the cause.
 */
final class InjectionFailure {

  private InjectionFailure() {}

  /** Describes a failed provisioning; {@code what} opens the message. */
  static IllegalStateException of(String what, ProvisionException failure) {
    return of(what, failure.getErrorMessages());
  }

  /** Describes a missing binding; {@code what} opens the message. */
  static IllegalStateException of(String what, ConfigurationException failure) {
    return of(what, failure.getErrorMessages());
  }

  private static IllegalStateException of(String what, Collection<Message> messages) {
    String text =
        messages.stream()
            .map(
                message ->
                    message.getCause() == null
                        ? message.getMessage()
                        : message.getMessage()
                            + " ("
                            + message.getCause().getClass().getSimpleName()
                            + ": "
                            + message.getCause().getMessage()
                            + ")")
            .collect(Collectors.joining("; "));
    return new IllegalStateException(what + ": " + text, firstCause(messages));
  }

  private static @Nullable Throwable firstCause(Collection<Message> messages) {
    return messages.stream()
        .map(Message::getCause)
        .filter(cause -> cause != null)
        .findFirst()
        .orElse(null);
  }
}
