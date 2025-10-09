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
    private UUID id;


    @ManyToOne(optional = false) @JoinColumn(name = "evidence_id")
    private Evidence evidence;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobType type;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;


    @Column(nullable = false)
    private int attempts;


    @Column(columnDefinition = "text")
    private String errorMsg;


    @Column(columnDefinition = "jsonb")
    private String payloadJson;


    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;


    @PrePersist void onCreate(){
        var now = Instant.now();
        this.createdAt = now; this.updatedAt = now;
        if (this.status == null) this.status = JobStatus.QUEUED;
    }
    @PreUpdate void onUpdate(){ this.updatedAt = Instant.now(); }
}