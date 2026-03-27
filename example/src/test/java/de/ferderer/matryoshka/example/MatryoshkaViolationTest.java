package de.ferderer.matryoshka.example;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import de.ferderer.matryoshka.example.rules.MatryoshkaRules;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Negative tests: each rule from MatryoshkaRules must detect its corresponding violation.
 * Violation classes live in de.ferderer.matryoshka.example.violations — one per rule.
 */
class MatryoshkaViolationTest {

    private static final MatryoshkaRules RULES =
        new MatryoshkaRules("de.ferderer.matryoshka.example") {};

    // Include violation classes (test scope) + config production classes (needed for V1)
    private static final ImportOption VIOLATIONS_AND_CONFIG =
        location -> !location.contains("/test/") || location.contains("/violations/");

    private static final JavaClasses WITH_CONFIG = new ClassFileImporter()
        .withImportOption(VIOLATIONS_AND_CONFIG)
        .importPackages(
            "de.ferderer.matryoshka.example.violations",
            "de.ferderer.matryoshka.example.config"
        );

    private static final JavaClasses VIOLATIONS_ONLY = new ClassFileImporter()
        .withImportOption(new ImportOption.DoNotIncludeJars())
        .importPackages("de.ferderer.matryoshka.example.violations");

    @Test
    void detects_config_not_at_top_level() {
        assertThatThrownBy(() -> RULES.config_top_level_only.check(VIOLATIONS_ONLY))
            .isInstanceOf(AssertionError.class);
    }

    @Test
    void detects_dependency_on_config() {
        assertThatThrownBy(() -> MatryoshkaRules.no_dependency_on_config.check(WITH_CONFIG))
            .isInstanceOf(AssertionError.class);
    }

    @Test
    void detects_common_accessed_outside_scope() {
        assertThatThrownBy(() -> MatryoshkaRules.common_only_within_scope.check(VIOLATIONS_ONLY))
            .isInstanceOf(AssertionError.class);
    }

    @Test
    void detects_transactional_on_service() {
        assertThatThrownBy(() -> MatryoshkaRules.no_transactional_on_bc_services.check(VIOLATIONS_ONLY))
            .isInstanceOf(AssertionError.class);
    }

    @Test
    void detects_controller_missing_transactional() {
        assertThatThrownBy(() -> MatryoshkaRules.controllers_own_transaction_boundary.check(VIOLATIONS_ONLY))
            .isInstanceOf(AssertionError.class);
    }

    @Test
    void detects_repository_outside_common_persistence() {
        assertThatThrownBy(() -> MatryoshkaRules.repositories_in_common_persistence.check(VIOLATIONS_ONLY))
            .isInstanceOf(AssertionError.class);
    }
}
