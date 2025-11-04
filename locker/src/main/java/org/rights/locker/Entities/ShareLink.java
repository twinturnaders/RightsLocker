package org.rights.locker.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "share_link", indexes = {
        @Index(name = "ux_share_token", columnList = "token", unique = true),
        @Index(name = "ix_share_evidence_id", columnList = "evidence_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShareLink {

    @Id @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 140)
    private String token;              // URL-safe

    @Column(name = "evidence_id", nullable = false)
    private UUID evidenceId;

    @Column(nullable = false)
    private boolean allowOriginal;

    // When this share stops working for the public (capability expires)
    @Column(nullable = false)
    private Instant expiresAt;

    // If non-null, token is revoked (can be set even before expiresAt)
    private Instant revokedAt;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    @Transient
    public boolean isActive() {
        var now = Instant.now();
        return revokedAt == null && (expiresAt == null || expiresAt.isAfter(now));
    }
}