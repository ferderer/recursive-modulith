package de.ferderer.matryoshka.example.policy.common.domain;

import java.util.UUID;

public record PolicyDraft(UUID id, String holderName, PolicyStatus status) {

    public static PolicyDraft create(UUID id, String holderName) {
        return new PolicyDraft(id, holderName, PolicyStatus.DRAFT);
    }
}
