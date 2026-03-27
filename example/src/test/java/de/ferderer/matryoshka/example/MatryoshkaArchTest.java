package de.ferderer.matryoshka.example;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import de.ferderer.matryoshka.example.rules.MatryoshkaRules;

@AnalyzeClasses(
    packages = "de.ferderer.matryoshka.example",
    importOptions = {
        ImportOption.DoNotIncludeTests.class,
        ImportOption.DoNotIncludeJars.class
    }
)
class MatryoshkaArchTest extends MatryoshkaRules {

    MatryoshkaArchTest() {
        super("de.ferderer.matryoshka.example");
    }
}
