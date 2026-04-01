package org.rights.locker.Services;

import org.junit.jupiter.api.Test;
import org.rights.locker.DTOs.AuthenticityAssessment;
import org.rights.locker.DTOs.MediaMetadata;
import org.rights.locker.Enums.MetadataIntegrity;
import org.rights.locker.Enums.ProvenanceStatus;
import org.rights.locker.Enums.SyntheticMediaRisk;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticityAssessmentServiceTest {

    private final AuthenticityAssessmentService service = new AuthenticityAssessmentService();

    @Test
    void returnsHighRiskWhenMetadataIsMissing() {
        AuthenticityAssessment assessment = service.assess(null);

        assertEquals(ProvenanceStatus.NOT_PRESENT, assessment.provenanceStatus());
        assertEquals(MetadataIntegrity.MISSING, assessment.metadataIntegrity());
        assertEquals(SyntheticMediaRisk.HIGH, assessment.syntheticMediaRisk());
        assertTrue(assessment.manipulationSignals().contains("No metadata extracted"));
    }

    @Test
    void returnsLowRiskWhenMetadataIsPresentAndConsistent() {
        MediaMetadata metadata = new MediaMetadata(
                Instant.parse("2026-03-31T18:15:30Z"),
                -360,
                39.7392, -104.9903, 1609.3, 90.0,
                "Canon", "R6", "RF24-70mm", "CameraOS",
                4000, 3000, 0,
                null, null, null,
                null, null, null,
                null,
                Map.of("source", "test")
        );

        AuthenticityAssessment assessment = service.assess(metadata);

        assertEquals(ProvenanceStatus.VERIFIED, assessment.provenanceStatus());
        assertEquals(MetadataIntegrity.COMPLETE, assessment.metadataIntegrity());
        assertEquals(SyntheticMediaRisk.LOW, assessment.syntheticMediaRisk());
        assertTrue(assessment.manipulationSignals().isEmpty());
    }

    @Test
    void flagsPartialMetadataAndDeviceGaps() {
        MediaMetadata metadata = new MediaMetadata(
                null,
                null,
                null, null, null, null,
                null, null, null, "EditorX",
                1920, 1080, null,
                "mp4", "h264", "aac",
                1200L, 30.0, 0,
                null,
                Map.of()
        );

        AuthenticityAssessment assessment = service.assess(metadata);

        assertEquals(ProvenanceStatus.UNKNOWN, assessment.provenanceStatus());
        assertEquals(MetadataIntegrity.PARTIAL, assessment.metadataIntegrity());
        assertEquals(SyntheticMediaRisk.HIGH, assessment.syntheticMediaRisk());
        assertTrue(assessment.manipulationSignals().contains("Missing EXIF metadata"));
        assertTrue(assessment.manipulationSignals().contains("Missing device metadata"));
        assertTrue(assessment.manipulationSignals().contains("Missing original capture timestamp"));
    }
}
