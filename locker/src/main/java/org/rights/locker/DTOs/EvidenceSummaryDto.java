package org.rights.locker.DTOs;

import java.time.Instant;
import java.util.UUID;

public record EvidenceSummaryDto(
        UUID id,
        String title,
        Instant capturedAt,
        boolean legalHold,
        String status,
        OwnerDto owner
) {}
