package org.rights.locker.Entities;


import jakarta.persistence.*;
import lombok.*;
import org.rights.locker.Enums.Role;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "app_user")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser {
    @Id @GeneratedValue
    @Column(name = "id")
    private UUID id;


    @Column(nullable = false, unique = true, name = "email")
    private String email;


    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name")
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "role")
    private Role role; // USER, MOD, ADMIN


    @Column(nullable = false, name = "created_at")
    private Instant createdAt;


    @Column(nullable = false, name = "updated_at")
    private Instant updatedAt;


    @PrePersist void onCreate(){
        var now = Instant.now();
        this.createdAt = now; this.updatedAt = now;
        if (this.role == null) this.role = Role.valueOf("USER");
    }
    @PreUpdate void onUpdate(){ this.updatedAt = Instant.now(); }
}