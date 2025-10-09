package org.rights.locker.DTOs;

import java.time.Instant;
import java.util.UUID;

public record EvidenceResponse(
        UUID id,
        String title,
        String description,
        Instant capturedAt,
        String status,
        boolean legalHold,
        String derivativeUrl,
        String thumbnailUrl
) {}
