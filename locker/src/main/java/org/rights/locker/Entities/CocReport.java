package org.rights.locker.Entities;

import jakarta.persistence.*;
import lombok.*;
import org.rights.locker.Entities.Evidence;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "coc_report")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CocReport {
    @Id @GeneratedValue
    private UUID id;


    @ManyToOne(optional = false) @JoinColumn(name = "evidence_id")
    private Evidence evidence;


    @Column(nullable = false)
    private String pdfKey;


    @Column(nullable = false)
    private String sha256;


    @Column(nullable = false)
    private Instant createdAt;


    @PrePersist void onCreate(){ this.createdAt = Instant.now(); }
}