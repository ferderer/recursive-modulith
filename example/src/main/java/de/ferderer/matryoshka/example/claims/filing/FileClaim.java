package de.ferderer.matryoshka.example.claims.filing;

import de.ferderer.matryoshka.example.claims.common.domain.ClaimEntity;
import de.ferderer.matryoshka.example.claims.common.persistence.ClaimRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/claims")
@Transactional
@RequiredArgsConstructor
class FileClaim {

    record Request(String description) {}
    record Response(UUID id, String description) {}

    private final ClaimRepository repo;

    @PostMapping
    Response handle(@RequestBody Request req) {
        var entity = new ClaimEntity(UUID.randomUUID(), req.description());
        repo.save(entity);
        return new Response(entity.getId(), entity.getDescription());
    }
}
