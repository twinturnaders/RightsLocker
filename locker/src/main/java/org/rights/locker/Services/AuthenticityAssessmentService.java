package org.rights.locker.Services;

import org.rights.locker.DTOs.AuthenticityAssessment;
import org.rights.locker.DTOs.MediaMetadata;
import org.rights.locker.Enums.MetadataIntegrity;
import org.rights.locker.Enums.ProvenanceStatus;
import org.rights.locker.Enums.SyntheticMediaRisk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic metadata-based review signals.
 * This service does not attempt to prove authenticity and does not call external systems.
 */
@Service
public class AuthenticityAssessmentService {

    public AuthenticityAssessment assess(MediaMetadata metadata) {
        if (metadata == null) {
            return new AuthenticityAssessment(
                    ProvenanceStatus.NOT_PRESENT,
                    MetadataIntegrity.MISSING,
                    SyntheticMediaRisk.HIGH,
                    List.of("No metadata extracted"),
                    "Metadata extraction produced no usable metadata. Review should treat the file as high-risk until corroborated by other evidence."
            );
        }

        List<String> manipulationSignals = new ArrayList<>();
        int riskScore = 0;

        boolean hasTimestamp = metadata.dateOriginal() != null;
        boolean hasDeviceInfo = hasText(metadata.cameraMake()) || hasText(metadata.cameraModel());
        boolean hasExifLikeMetadata = hasTimestamp
                || metadata.lat() != null
                || metadata.lon() != null
                || hasDeviceInfo
                || hasText(metadata.software())
                || metadata.orientationDeg() != null;
        boolean hasVideoMetadata = hasText(metadata.container())
                || hasText(metadata.videoCodec())
                || metadata.durationMs() != null
                || metadata.videoFps() != null;
        boolean hasAnyMetadata = hasExifLikeMetadata || hasVideoMetadata;

        if (!hasExifLikeMetadata) {
            riskScore += 2;
            manipulationSignals.add("Missing EXIF metadata");
        }

        if (!hasDeviceInfo) {
            riskScore += 1;
            manipulationSignals.add("Missing device metadata");
        }

        MetadataIntegrity integrity = MetadataIntegrity.COMPLETE;
        if (!hasAnyMetadata) {
            integrity = MetadataIntegrity.MISSING;
        } else if (!hasTimestamp || !hasDeviceInfo) {
            integrity = MetadataIntegrity.PARTIAL;
        }

        if (!hasTimestamp) {
            if (integrity == MetadataIntegrity.COMPLETE) {
                integrity = MetadataIntegrity.PARTIAL;
            }
            manipulationSignals.add("Missing original capture timestamp");
        }

        boolean hasDimensions = metadata.widthPx() != null && metadata.heightPx() != null;
        boolean hasCoherentVideoData = !hasText(metadata.container())
                || (hasText(metadata.videoCodec()) && metadata.durationMs() != null);

        if (hasAnyMetadata && hasTimestamp && hasDeviceInfo && hasDimensions && hasCoherentVideoData) {
            // Consistent metadata lowers risk because several independent fields agree on basic capture context.
            riskScore -= 1;
        }

        if (hasText(metadata.software()) && !hasDeviceInfo) {
            manipulationSignals.add("Software tag present without device metadata");
            riskScore += 1;
        }

        if (metadata.lat() == null ^ metadata.lon() == null) {
            manipulationSignals.add("Incomplete GPS coordinate metadata");
            if (integrity == MetadataIntegrity.COMPLETE) {
                integrity = MetadataIntegrity.PARTIAL;
            }
        }

        if (!hasCoherentVideoData) {
            manipulationSignals.add("Incomplete video stream metadata");
            if (integrity == MetadataIntegrity.COMPLETE) {
                integrity = MetadataIntegrity.PARTIAL;
            }
            riskScore += 1;
        }

        SyntheticMediaRisk syntheticMediaRisk = toRisk(riskScore);
        ProvenanceStatus provenanceStatus = hasAnyMetadata
                ? (integrity == MetadataIntegrity.COMPLETE ? ProvenanceStatus.VERIFIED : ProvenanceStatus.UNKNOWN)
                : ProvenanceStatus.NOT_PRESENT;

        String assessmentSummary = buildSummary(provenanceStatus, integrity, syntheticMediaRisk, manipulationSignals, hasAnyMetadata);
        return new AuthenticityAssessment(provenanceStatus, integrity, syntheticMediaRisk, manipulationSignals, assessmentSummary);
    }

    private static SyntheticMediaRisk toRisk(int riskScore) {
        if (riskScore >= 3) {
            return SyntheticMediaRisk.HIGH;
        }
        if (riskScore >= 1) {
            return SyntheticMediaRisk.MEDIUM;
        }
        return SyntheticMediaRisk.LOW;
    }

    private static String buildSummary(
            ProvenanceStatus provenanceStatus,
            MetadataIntegrity integrity,
            SyntheticMediaRisk risk,
            List<String> signals,
            boolean hasAnyMetadata
    ) {
        if (!hasAnyMetadata) {
            return "No provenance metadata was present. The file should be reviewed manually because authenticity cannot be supported by metadata.";
        }

        if (signals.isEmpty()) {
            return "Metadata is present and internally consistent. The file shows low heuristic risk, but findings remain advisory and require human review.";
        }

        return "Provenance status is %s with %s metadata integrity and %s synthetic media risk. Signals: %s. This is a heuristic aid for human review, not a definitive authenticity determination."
                .formatted(provenanceStatus, integrity, risk, String.join("; ", signals));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
