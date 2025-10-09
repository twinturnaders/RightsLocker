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
    private UUID id;


    @ManyToOne(optional = false) @JoinColumn(name = "evidence_id")
    private Evidence evidence;


    @ManyToOne(optional = false) @JoinColumn(name = "created_by")
    private AppUser createdBy;


    @Column(nullable = false, unique = true)
    private String token;


    @Column(nullable = false)
    private Instant expiresAt;


    @Column(nullable = false)
    private boolean allowOriginal;


    private Instant revokedAt;
}