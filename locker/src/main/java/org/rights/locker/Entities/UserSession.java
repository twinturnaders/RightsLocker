package org.rights.locker.Entities;


import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "user_session")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserSession {
    @Id @GeneratedValue
    private UUID id;


    @ManyToOne(optional = false) @JoinColumn(name = "user_id")
    private AppUser user;


    @Column(nullable = false)
    private String jwtId;


    @Column(nullable = false)
    private Instant createdAt;


    @Column(nullable = false)
    private Instant expiresAt;


    @PrePersist void onCreate(){ this.createdAt = Instant.now(); }
}