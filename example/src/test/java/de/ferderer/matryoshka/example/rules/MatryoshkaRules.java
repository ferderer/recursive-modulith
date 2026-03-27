package de.ferderer.matryoshka.example.rules;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static de.ferderer.matryoshka.example.rules.MatryoshkaConditions.onlyBeAccessedFromWithinScope;
import static de.ferderer.matryoshka.example.rules.MatryoshkaConditions.resideInTopLevelConfigPackage;

public class MatryoshkaRules {

    // config_top_level_only is an instance field because it requires the root package,
    // which is provided by the subclass via the constructor.
    // All other rules are package-agnostic and can be static.

    @ArchTest
    public final ArchRule config_top_level_only;

    protected MatryoshkaRules(String rootPackage) {
        config_top_level_only = classes().that().resideInAPackage("..config..")
            .should(resideInTopLevelConfigPackage(rootPackage))
            .as("Framework config must only exist at the top-level config package.");
    }

    // -------------------------------------------------------------------------
    // 1. config: never imported by domain code
    // -------------------------------------------------------------------------

    @ArchTest
    public static final ArchRule no_dependency_on_config =
        noClasses().that().resideOutsideOfPackage("..config..")
            .should().dependOnClassesThat().resideInAPackage("..config..")
            .as("Functional code must never depend on config.");

    // -------------------------------------------------------------------------
    // 2. common: visible within its own scope only (recursive, all levels)
    // -------------------------------------------------------------------------

    @ArchTest
    public static final ArchRule common_only_within_scope =
        classes().that().resideInAPackage("..common..")
            .should(onlyBeAccessedFromWithinScope())
            .as("'common' is only visible within its own scope (recursive rule).");

    // -------------------------------------------------------------------------
    // 3. Use Case isolation
    // -------------------------------------------------------------------------

    @ArchTest
    public static final ArchRule no_inter_use_case_dependencies =
        noClasses().that().areAnnotatedWith(RestController.class)
            .should().dependOnClassesThat().areAnnotatedWith(RestController.class)
            .as("Direct dependencies between Use Cases (Single-Action Controllers) are forbidden.");

    // -------------------------------------------------------------------------
    // 4. Transaction boundary: only on Single-Action Controllers
    // -------------------------------------------------------------------------

    @ArchTest
    public static final ArchRule no_transactional_on_bc_services =
        methods().that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Service")
            .should().notBeAnnotatedWith(Transactional.class)
            .as("BC Services must only delegate — @Transactional belongs on the Use Case.");

    @ArchTest
    public static final ArchRule controllers_own_transaction_boundary =
        classes().that().areAnnotatedWith(RestController.class)
            .should().beAnnotatedWith(Transactional.class)
            .as("Every Single-Action Controller must define its own transaction boundary.");

    // -------------------------------------------------------------------------
    // 5. Repositories must live in common/persistence
    // -------------------------------------------------------------------------

    @ArchTest
    public static final ArchRule repositories_in_common_persistence =
        classes().that().haveSimpleNameEndingWith("Repository")
            .should().resideInAPackage("..common.persistence..")
            .as("Repositories must live in common/persistence of their scope.");
}
