package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.port.GlobalConfigProvider;
import com.sitepark.ies.aggregator.port.HomepageProvider;
import com.sitepark.ies.aggregator.port.ObjectTypeConfigProvider;

/**
 * The ports that answer for the whole installation rather than for the object being aggregated:
 * the object-type configuration, the global configuration and the home pages.
 *
 * @param objectTypeConfigProvider the configuration of the object types
 * @param globalConfigProvider the installation's global configuration
 * @param homepageProvider the home pages of the installation
 */
record InstallationPorts(
    ObjectTypeConfigProvider objectTypeConfigProvider,
    GlobalConfigProvider globalConfigProvider,
    HomepageProvider homepageProvider) {}
