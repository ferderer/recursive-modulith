package de.ferderer.matryoshka.example.claims.common.persistence;

import de.ferderer.matryoshka.example.claims.common.domain.ClaimEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClaimRepository extends JpaRepository<ClaimEntity, UUID> {}
