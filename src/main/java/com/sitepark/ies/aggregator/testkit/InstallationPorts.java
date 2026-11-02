package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.port.CategoryRootProvider;
import com.sitepark.ies.aggregator.port.GlobalConfigProvider;
import com.sitepark.ies.aggregator.port.HomepageProvider;
import com.sitepark.ies.aggregator.port.ObjectTypeConfigProvider;

/**
 * The ports that answer for the whole installation rather than for the object being aggregated:
 * the object-type configuration, the global configuration, the home pages and the category roots.
 *
 * @param objectTypeConfigProvider the configuration of the object types
 * @param globalConfigProvider the installation's global configuration
 * @param homepageProvider the home pages of the installation
 * @param categoryRootProvider the category roots of the installation
 */
record InstallationPorts(
    ObjectTypeConfigProvider objectTypeConfigProvider,
    GlobalConfigProvider globalConfigProvider,
    HomepageProvider homepageProvider,
    CategoryRootProvider categoryRootProvider) {}
