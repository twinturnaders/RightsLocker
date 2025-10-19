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
    @Column(name = "id")
    private UUID id;


    @Column(nullable = false, name = "provider")
    private String provider;


    @Column(nullable = false, name = "event_type")
    private String eventType;


    @Column(nullable = false, name = "external_id")
    private String externalId;


    @Column(columnDefinition = "jsonb", nullable = false, name = "payload_json")
    private String payloadJson;


    @Column(nullable = false, name = "processed")
    private boolean processed;

    @Column(name="processed_at")
    private Instant processedAt;
}