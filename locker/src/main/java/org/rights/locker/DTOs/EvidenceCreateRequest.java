package org.rights.locker.DTOs;

import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;


public record EvidenceCreateRequest(
        @NotNull MultipartFile file,
        String title,
        String description,
        Instant capturedAt,
        Double lat,
        Double lon,
        Double accuracy
) {}


