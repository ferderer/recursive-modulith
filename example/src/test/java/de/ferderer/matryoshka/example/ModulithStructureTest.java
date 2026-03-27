package de.ferderer.matryoshka.example;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithStructureTest {

    @Test
    void verifyModulithStructure() {
        ApplicationModules.of(Application.class).verify();
    }
}
