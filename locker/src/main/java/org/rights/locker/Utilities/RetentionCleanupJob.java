package org.rights.locker.Utilities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rights.locker.Config.AppProps;
import org.rights.locker.Entities.Evidence;
import org.rights.locker.Repos.EvidenceRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetentionCleanupJob {

    private final AppProps props;
    private final EvidenceRepo evidenceRepo;
    private final S3Client s3;

    @Value("${app.s3.bucketOriginals}") private String bucketOriginals;
    @Value("${app.s3.bucketHot}") private String bucketHot;

    // run every 5 minutes
    @Scheduled(fixedDelay = 300_000L, initialDelay = 120_000L)
    public void sweep() {
        if (!"processing".equalsIgnoreCase(props.getMode())) return;

        Instant cutoff = Instant.now().minus(props.getRetentionHours(), ChronoUnit.HOURS);

        // you may want a repo method like: findByCreatedAtBefore(cutoff)
        List<Evidence> expired = evidenceRepo.findAll().stream()
                .filter(ev -> ev.getCreatedAt() != null && ev.getCreatedAt().isBefore(cutoff))
                .toList();

        if (expired.isEmpty()) return;
        log.info("Retention sweep: {} evidence items to purge (cutoff: {})", expired.size(), cutoff);

        for (Evidence ev : expired) {
            try {
                if (ev.getOriginalKey() != null) {
                    s3.deleteObject(DeleteObjectRequest.builder()
                            .bucket(bucketOriginals).key(ev.getOriginalKey()).build());
                }
                if (ev.getThumbnailKey() != null) {
                    s3.deleteObject(DeleteObjectRequest.builder()
                            .bucket(bucketHot).key(ev.getThumbnailKey()).build());
                }
                if (ev.getRedactedKey() != null) {
                    s3.deleteObject(DeleteObjectRequest.builder()
                            .bucket(bucketHot).key(ev.getRedactedKey()).build());
                }
                evidenceRepo.delete(ev);
                log.info("Purged evidence {} ({})", ev.getId(), ev.getTitle());
            } catch (Exception ex) {
                log.warn("Failed to purge evidence {}: {}", ev.getId(), ex.getMessage());
            }
        }
    }
}