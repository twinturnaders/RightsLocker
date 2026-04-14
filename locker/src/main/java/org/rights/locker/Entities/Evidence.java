package org.rights.locker.Entities;



import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;
import org.rights.locker.Enums.EvidenceStatus;
import org.rights.locker.Enums.MetadataIntegrity;
import org.rights.locker.Enums.ProvenanceStatus;
import org.rights.locker.Enums.SyntheticMediaRisk;
import org.rights.locker.Utilities.StringListJsonConverter;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "evidence")
@NoArgsConstructor @AllArgsConstructor @Builder
@Getter
@Setter
@ToString
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
    private BigDecimal captureAccuracyM;


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

    @Column(name = "redacted_size")
    private Long redactedSize;
    @Column(nullable = false, name= "legal_hold")
    private Boolean legalHold;


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
    @Column(name = "exif_date_original")
    private Instant exifDateOriginal;

    @Column(name = "tz_offset_minutes")
    private Integer tzOffsetMinutes;

    @Column(name = "capture_altitude_m")
    private Double captureAltitudeM;

    @Column(name = "capture_heading_deg")
    private Double captureHeadingDeg;

    @Column(name = "camera_make")
    private String cameraMake;

    @Column(name = "camera_model")
    private String cameraModel;

    @Column(name = "lens_model")
    private String lensModel;

    @Column(name = "software")
    private String software;

    @Column(name = "width_px")
    private Integer widthPx;

    @Column(name = "height_px")
    private Integer heightPx;

    @Column(name = "orientation_deg")
    private Integer orientationDeg;

    @Column(name = "container")
    private String container;

    @Column(name = "video_codec")
    private String videoCodec;

    @Column(name = "audio_codec")
    private String audioCodec;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "video_fps")
    private Double videoFps;

    @Column(name = "video_rotation_deg")
    private Integer videoRotationDeg;

    @Enumerated(EnumType.STRING)
    @Column(name = "provenance_status")
    private ProvenanceStatus provenanceStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "metadata_integrity")
    private MetadataIntegrity metadataIntegrity;

    @Enumerated(EnumType.STRING)
    @Column(name = "synthetic_media_risk")
    private SyntheticMediaRisk syntheticMediaRisk;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "manipulation_signals", columnDefinition = "TEXT")
    private List<String> manipulationSignals;

    @Column(name = "assessment_summary", columnDefinition = "TEXT")
    private String assessmentSummary;

    public UUID getOwnerId() {
        return owner != null ? owner.getId() : null;
    }
}
