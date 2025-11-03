package org.rights.locker.Entities;



import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.rights.locker.Enums.CustodyEventType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;


@Entity
@Table(name = "custody_event")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustodyEvent {
    @Id @GeneratedValue
    @Column(name = "id")
    private UUID id;


    @ManyToOne(optional = false) @JoinColumn(name = "evidence_id")
    private Evidence evidence;


    @ManyToOne @JoinColumn(name = "actor_user_id")
    private AppUser actor;


    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private CustodyEventType eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "meta_Json")
    private Map<String, Object> metaJson;;


    @Column(nullable = false, name = "created_at")
    private Instant createdAt;


    @PrePersist void onCreate(){ this.createdAt = Instant.now(); }
}