package de.ferderer.matryoshka.example.violations;

import de.ferderer.matryoshka.example.config.WebConfig;

/**
 * VIOLATION V1: Domain code must never depend on config.
 * Expected: config_top_level_only / no_dependency_on_config fires.
 */
class DomainClassImportingConfig {
    // Illegal reference to a config class
    private final WebConfig webConfig = null;
}
