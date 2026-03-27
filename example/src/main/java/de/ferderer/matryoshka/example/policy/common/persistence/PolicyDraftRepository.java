package de.ferderer.matryoshka.example.policy.common.persistence;

import de.ferderer.matryoshka.example.policy.common.domain.PolicyDraftEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PolicyDraftRepository extends JpaRepository<PolicyDraftEntity, UUID> {}
