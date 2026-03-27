package de.ferderer.matryoshka.example.violations;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

/**
 * VIOLATION V5: Repository outside common/persistence.
 * Expected: repositories_in_common_persistence fires.
 */
interface MisplacedRepository extends JpaRepository<Object, UUID> {}
