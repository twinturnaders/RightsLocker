package org.rights.locker.Entities;



import jakarta.persistence.*;
import lombok.*;
import org.rights.locker.Enums.JobStatus;
import org.rights.locker.Enums.JobType;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "processing_job")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProcessingJob {
    @Id @GeneratedValue
    @Column(name = "id")
    private UUID id;


    @ManyToOne(optional = false) @JoinColumn(name = "evidence_id")
    private Evidence evidence;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "type")
    private JobType type;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "status")
    private JobStatus status;


    @Column(nullable = false, name = "attempts")
    private int attempts;


    @Column(columnDefinition = "text", name = "error_msg")
    private String errorMsg;


    @Column(columnDefinition = "jsonb", name = "payload_json")
    private String payloadJson;


    @Column(nullable = false, name = "created_at")
    private Instant createdAt;
    @Column(nullable = false, name = "updated_at")
    private Instant updatedAt;


    @PrePersist void onCreate(){
        var now = Instant.now();
        this.createdAt = now; this.updatedAt = now;
        if (this.status == null) this.status = JobStatus.QUEUED;
    }
    @PreUpdate void onUpdate(){ this.updatedAt = Instant.now(); }

    public void setFinishedAt(Instant now) {
        this.updatedAt = now;
    }

    public void setLastError(String error) {
        this.errorMsg = error;
    }

    public void setStartedAt(Instant now) {
        this.createdAt = now;
    }

    public void setQueuedAt(Instant now) {
        this.updatedAt = now;
    }
}