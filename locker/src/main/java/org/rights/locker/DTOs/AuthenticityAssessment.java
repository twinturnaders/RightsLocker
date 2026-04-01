package org.rights.locker.DTOs;

import org.rights.locker.Enums.MetadataIntegrity;
import org.rights.locker.Enums.ProvenanceStatus;
import org.rights.locker.Enums.SyntheticMediaRisk;

import java.util.List;

/**
 * Heuristic assessment intended to support human review.
 * It is deterministic and explainable, and should not be treated as a definitive AI detector.
 */
public record AuthenticityAssessment(
        ProvenanceStatus provenanceStatus,
        MetadataIntegrity metadataIntegrity,
        SyntheticMediaRisk syntheticMediaRisk,
        List<String> manipulationSignals,
        String assessmentSummary
) {
    public AuthenticityAssessment {
        manipulationSignals = manipulationSignals == null ? List.of() : List.copyOf(manipulationSignals);
    }
}
