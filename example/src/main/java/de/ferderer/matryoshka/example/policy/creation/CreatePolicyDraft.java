package de.ferderer.matryoshka.example.policy.creation;

import de.ferderer.matryoshka.example.policy.PolicyCreatedEvent;
import de.ferderer.matryoshka.example.policy.common.domain.PolicyDraft;
import de.ferderer.matryoshka.example.policy.common.domain.PolicyDraftEntity;
import de.ferderer.matryoshka.example.policy.common.persistence.PolicyDraftRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/policies/drafts")
@Transactional
@RequiredArgsConstructor
class CreatePolicyDraft {

    record Request(String holderName) {}
    record Response(UUID id, String holderName, String status) {}

    private final PolicyDraftRepository repo;
    private final ApplicationEventPublisher events;

    @PostMapping
    Response handle(@RequestBody Request req) {
        var draft = PolicyDraft.create(UUID.randomUUID(), req.holderName());
        repo.save(PolicyDraftEntity.fromDomain(draft));
        events.publishEvent(new PolicyCreatedEvent(draft.id(), draft.holderName()));
        return new Response(draft.id(), draft.holderName(), draft.status().name());
    }
}
