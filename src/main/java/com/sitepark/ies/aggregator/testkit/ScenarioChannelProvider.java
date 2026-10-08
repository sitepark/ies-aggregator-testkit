package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.port.Channel;
import com.sitepark.ies.aggregator.port.ChannelProvider;
import com.sitepark.ies.aggregator.value.AccessRestriction;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * {@link ChannelProvider} that always reports a single, fixed publication channel as active.
 *
 * <p>The channels are per scenario rather than shared constants, because what a channel says about
 * an object — who may see it — is part of what a scenario describes.
 */
final class ScenarioChannelProvider implements ChannelProvider {

  private final ScenarioChannel currentChannel;
  private final ScenarioChannel primaryChannel;
  private final boolean hasPrimaryChannel;

  /**
   * @param repository the scenario's repository, which tells the channels which objects are
   *     standalone media and therefore have no page URL
   * @param accessRestriction the restriction both channels report, or {@code null} for an
   *     unrestricted resource
   * @param config the nature and attributes both channels report
   */
  ScenarioChannelProvider(
      Repository repository,
      @Nullable AccessRestriction accessRestriction,
      ScenarioChannelConfig config) {
    this.currentChannel =
        new ScenarioChannel(1, true, "Current Channel", accessRestriction, repository, config);
    this.primaryChannel =
        new ScenarioChannel(2, false, "Primary Channel", accessRestriction, repository, config);
    this.hasPrimaryChannel = config.primaryChannel();
  }

  /**
   * The primary channel only where the scenario declares one ({@code "primaryChannel": true}).
   * Production answers empty for an object no pool assigns a primary channel, and that is the usual
   * case; answering one regardless would let a fallback through that production never takes.
   */
  @Override
  public Optional<Channel> primary(int id) {
    return this.hasPrimaryChannel ? Optional.of(this.primaryChannel) : Optional.empty();
  }

  @Override
  public Channel current() {
    return this.currentChannel;
  }

  @Override
  public Optional<Channel> get(int id) {
    if (id == 1) {
      return Optional.of(this.currentChannel);
    }
    if (id == 2) {
      return Optional.of(this.primaryChannel);
    }
    return Optional.empty();
  }
}
