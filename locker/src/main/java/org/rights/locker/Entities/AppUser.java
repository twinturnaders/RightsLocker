package org.rights.locker.Entities;


import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "app_user")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser {
    @Id @GeneratedValue
    private UUID id;


    @Column(nullable = false, unique = true)
    private String email;


    @Column(name = "password_hash", nullable = false)
    private String passwordHash;


    private String displayName;


    @Column(nullable = false)
    private String role; // USER, MOD, ADMIN


    @Column(nullable = false)
    private Instant createdAt;


    @Column(nullable = false)
    private Instant updatedAt;


    @PrePersist void onCreate(){
        var now = Instant.now();
        this.createdAt = now; this.updatedAt = now;
        if (this.role == null) this.role = "USER";
    }
    @PreUpdate void onUpdate(){ this.updatedAt = Instant.now(); }
}