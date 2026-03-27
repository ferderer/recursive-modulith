package de.ferderer.matryoshka.example.policy.common.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

@Entity(name = "PolicyDraft")
@Table(name = "policy_drafts")
@Getter
@Setter
@AllArgsConstructor
public class PolicyDraftEntity {

    @Id
    private UUID id;
    private String holderName;

    @Enumerated(EnumType.STRING)
    private PolicyStatus status;

    public static PolicyDraftEntity fromDomain(PolicyDraft draft) {
        return new PolicyDraftEntity(draft.id(), draft.holderName(), draft.status());
    }

    public PolicyDraft toDomain() {
        return new PolicyDraft(id, holderName, status);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PolicyDraftEntity other && Objects.equals(id, other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
