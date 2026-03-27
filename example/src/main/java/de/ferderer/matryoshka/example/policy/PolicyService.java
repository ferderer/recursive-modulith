package de.ferderer.matryoshka.example.policy;

import de.ferderer.matryoshka.example.policy.common.domain.PolicyDraft;
import de.ferderer.matryoshka.example.policy.common.domain.PolicyDraftEntity;
import de.ferderer.matryoshka.example.policy.common.persistence.PolicyDraftRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyDraftRepository repo;

    public Optional<PolicyDraft> findDraft(UUID id) {
        return repo.findById(id).map(PolicyDraftEntity::toDomain);
    }
}
