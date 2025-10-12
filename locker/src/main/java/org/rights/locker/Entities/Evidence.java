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
    private UUID id;


    @ManyToOne @JoinColumn(name = "owner_user_id")
    private AppUser owner;


    private String title;
    private String description;


    private Instant capturedAt;


    @Column(columnDefinition = "geography(Point,4326)")
    private Point captureLatlon;


    private Double captureAccuracyM;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvidenceStatus status;


    @Column(nullable = false)
    private String originalSha256;


    @Column(nullable = false)
    private Long originalSizeB;


    @Column(nullable = false)
    private String originalKey; // s3 key (originals)


    private String derivativeKey; // s3 key (hot)

    @Column(nullable = true)
    private String thumbnailKey;

    @Column(nullable = true)
    private String redactedKey;


    @Column(nullable = false)
    private boolean legalHold;


    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;


    @PrePersist void onCreate(){
        var now = Instant.now();
        this.createdAt = now; this.updatedAt = now;
        if (this.status == null) this.status = EvidenceStatus.RECEIVED;
    }
    @PreUpdate void onUpdate(){ this.updatedAt = Instant.now(); }
}