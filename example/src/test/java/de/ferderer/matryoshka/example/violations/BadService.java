package de.ferderer.matryoshka.example.violations;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * VIOLATION V3: BC Service must not declare @Transactional.
 * Expected: no_transactional_on_bc_services fires.
 */
@Service
class BadService {

    @Transactional
    public void doSomething() {
        // Transaction boundary must live on the Use Case, not here
    }
}
