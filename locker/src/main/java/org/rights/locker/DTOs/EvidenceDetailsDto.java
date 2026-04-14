package org.rights.locker.DTOs;


import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EvidenceDetailsDto(
        UUID id,
        OwnerDto owner,
        String title,

        String description,


        Instant capturedAt,



        Point captureLatlon,

        BigDecimal captureAccuracyM,


        org.rights.locker.Enums.EvidenceStatus status,

        String originalSha256,

        Long originalSizeB,

        String originalKey, // s3 key (originals)

        String derivativeKey, // s3 key (hot)

        String thumbnailKey,

        String redactedKey,

        Long redactedSize,
        Boolean legalHold,


        Instant createdAt,
        Instant updatedAt,
        Instant exifDateOriginal,
        Integer tzOffsetMinutes,
        Double captureAltitudeM,
        Double captureHeadingDeg,
        String cameraMake,
        String cameraModel,
        String lensModel,
        String software,
        Integer widthPx,
        Integer heightPx,
        Integer orientationDeg,


        String container,


        String videoCodec,


        String audioCodec,
        Long durationMs,
        Double videoFps,
        Integer videoRotationDeg,
        AuthenticityAssessment authenticityAssessment
) {}
