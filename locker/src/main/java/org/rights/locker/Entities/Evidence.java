package org.rights.locker.Entities;



import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;
import org.rights.locker.Enums.EvidenceStatus;


import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "evidence")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Evidence {
    @Id @GeneratedValue
    @Column(name = "id")
    private UUID id;


    @ManyToOne @JoinColumn(name = "owner_user_id")
    private AppUser owner;

    @Column(name="title")
    private String title;
    @Column(name="description")
    private String description;

    @Column(name = "captured_at")
    private Instant capturedAt;


    @Column(columnDefinition = "geography(Point,4326)")
    private Point captureLatlon;

    @Column(name="capture_accuracy_m")
    private Double captureAccuracyM;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "status")
    private EvidenceStatus status;


    @Column(nullable = false, name = "original_sha256")
    private String originalSha256;


    @Column(nullable = false, name = "original_size_b")
    private Long originalSizeB;


    @Column(nullable = false, name = "original_key")
    private String originalKey; // s3 key (originals)

    @Column(name = "derivative_key")
    private String derivativeKey; // s3 key (hot)

    @Column(nullable = true, name = "thumbnail_key")
    private String thumbnailKey;

    @Column(nullable = true, name = "redacted_key")
    private String redactedKey;


    @Column(nullable = false, name= "legal_hold")
    private boolean legalHold;


    @Column(nullable = false, name = "created_at")
    private Instant createdAt;
    @Column(nullable = false, name = "updated_at")
    private Instant updatedAt;


    @PrePersist void onCreate(){
        var now = Instant.now();
        this.createdAt = now; this.updatedAt = now;
        if (this.status == null) this.status = EvidenceStatus.RECEIVED;
    }
    @PreUpdate void onUpdate(){ this.updatedAt = Instant.now(); }
}