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
        new ScenarioChannel(1, "Current Channel", accessRestriction, repository, config);
    this.primaryChannel =
        new ScenarioChannel(2, "Primary Channel", accessRestriction, repository, config);
  }

  @Override
  public Optional<Channel> primary(int id) {
    return Optional.of(this.primaryChannel);
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
