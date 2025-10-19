package org.rights.locker.Entities;


import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "share_link")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShareLink {
    @Id @GeneratedValue
    @Column(name = "id")
    private UUID id;


    @ManyToOne(optional = false) @JoinColumn(name = "evidence_id")
    private Evidence evidence;


    @ManyToOne(optional = false) @JoinColumn(name = "created_by")
    private AppUser createdBy;


    @Column(nullable = false, unique = true, name = "token")
    private String token;


    @Column(nullable = false, name= "expires_at")
    private Instant expiresAt;


    @Column(nullable = false, name = "allow_original")
    private boolean allowOriginal;

    @Column(name = "revoked_at")
    private Instant revokedAt;
}