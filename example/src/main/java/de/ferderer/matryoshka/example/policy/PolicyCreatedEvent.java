package de.ferderer.matryoshka.example.policy;

import java.util.UUID;

public record PolicyCreatedEvent(UUID policyId, String holderName) {}
