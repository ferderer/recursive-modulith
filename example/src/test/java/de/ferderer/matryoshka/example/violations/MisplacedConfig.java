package de.ferderer.matryoshka.example.violations.config;

import org.springframework.context.annotation.Configuration;

/**
 * VIOLATION V1a: Config class inside a non-root package (simulates bc.config).
 * Expected: config_top_level_only fires — config must only live under the root package.
 */
@Configuration
public class MisplacedConfig {}
