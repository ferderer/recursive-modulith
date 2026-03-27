package de.ferderer.matryoshka.example.claims.common.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

@Entity(name = "Claim")
@Table(name = "claims")
@Getter
@Setter
@AllArgsConstructor
public class ClaimEntity {

    @Id
    private UUID id;
    private String description;

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ClaimEntity other && Objects.equals(id, other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
