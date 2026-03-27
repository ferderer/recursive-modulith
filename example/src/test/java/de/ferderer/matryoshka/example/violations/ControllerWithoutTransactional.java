package de.ferderer.matryoshka.example.violations;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * VIOLATION V4: Single-Action Controller without @Transactional.
 * Expected: controllers_own_transaction_boundary fires.
 */
@RestController
@RequestMapping("/api/v1/violations/missing-tx")
class ControllerWithoutTransactional {

    @GetMapping
    String handle() {
        return "oops — no transaction boundary defined";
    }
}
