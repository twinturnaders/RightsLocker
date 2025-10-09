package org.rights.locker.Entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "webhook_event")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WebhookEvent {
    @Id @GeneratedValue
    private UUID id;


    @Column(nullable = false)
    private String provider;


    @Column(nullable = false)
    private String eventType;


    @Column(nullable = false)
    private String externalId;


    @Column(columnDefinition = "jsonb", nullable = false)
    private String payloadJson;


    @Column(nullable = false)
    private boolean processed;


    private Instant processedAt;
}