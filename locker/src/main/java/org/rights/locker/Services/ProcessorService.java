// File: src/main/java/org/rights/locker/Services/ProcessorService.java
package org.rights.locker.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rights.locker.Config.RabbitConfig;
import org.rights.locker.Entities.ProcessingJob;
import org.rights.locker.Enums.JobStatus;
import org.rights.locker.Enums.JobType;
import org.rights.locker.Repos.EvidenceRepo;
import org.rights.locker.Repos.ProcessingJobRepo;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import static org.bytedeco.librealsense.global.RealSense.none;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessorService {

    private final RabbitTemplate rabbit;        // uses default exchange ""
    private final ProcessingJobRepo jobs;
    private final EvidenceRepo evidenceRepo;
    private final CustodyService custody;       // if you have chain-of-custody events
    private final StorageService storage;       // in case you need to poke S3 in completion

    /**
     * Message sent to the worker queue.
     * Keep it small & serializable (Jackson2JsonMessageConverter recommended).
     */
    public record JobMessage(UUID jobId, JobType type, UUID evidenceId, int attempt) implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
    }


    public void publish(ProcessingJob job) {

        job.setAttempts((job.getAttempts() == none) ? 1 : job.getAttempts() + 1);
        job.setStatus(JobStatus.QUEUED);

        jobs.save(job);

        var msg = new JobMessage(job.getId(), job.getType(), job.getEvidence().getId(), job.getAttempts());
        log.info("Publishing job {} type={} ev={} attempt={}", job.getId(), job.getType(), job.getEvidence().getId(), job.getAttempts());
        // default exchange (""), routingKey = queue name
        rabbit.convertAndSend("", RabbitConfig.JOBS_QUEUE, msg);
    }

    /**
     * Mark job as started (consumer can call this when it begins work).
     */
    public ProcessingJob markStarted(UUID jobId) {
        var job = jobs.findById(jobId).orElseThrow();
        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        return jobs.save(job);
    }

    /**
     * Mark job successful, attach outputs, and update Evidence depending on job type.
     * For THUMBNAIL: set ev.thumbnailKey
     * For REDACT:    set ev.redactedKey, optionally ev.redactedSizeB & ev.redactedSha256
     */
    public void completeSuccess(UUID jobId, String outputKey, Long outputSizeB, String outputSha256) {
        var job = jobs.findById(jobId).orElseThrow();
        job.setStatus(JobStatus.SUCCESS);
        job.setUpdatedAt(Instant.now());
        jobs.save(job);

        var ev = job.getEvidence();
        switch (job.getType()) {
            case THUMBNAIL -> {
                ev.setThumbnailKey(outputKey);
                // (optional) ev.setThumbnailSizeB(outputSizeB);
                custody.record(ev, null, org.rights.locker.Enums.CustodyEventType.THUMBNAIL_GENERATED,
                        "{\"key\":\"" + outputKey + "\"}");
            }
            case REDACT -> {
                ev.setRedactedKey(outputKey);
                if (outputSizeB != null) ev.setRedactedSizeB(outputSizeB);
                if (outputSha256 != null) ev.setRedactedKey(outputSha256);
                // you may also update status if your Evidence tracks lifecycle
                // ev.setStatus(EvidenceStatus.REDACTED);
                custody.record(ev, null, org.rights.locker.Enums.CustodyEventType.REDACTED,
                        "{\"key\":\"" + outputKey + "\",\"sha256\":\"" + safe(outputSha256) + "\"}");
            }
            default -> log.warn("completeSuccess: unhandled job type {}", job.getType());
        }
        evidenceRepo.save(ev);
        log.info("Job {} completed OK; outputKey={}", jobId, outputKey);
    }

    /**
     * Mark job failed and keep the error for diagnostics; you can requeue elsewhere if desired.
     */
    public void completeFailure(UUID jobId, String error) {
        var job = jobs.findById(jobId).orElseThrow();
        job.setStatus(JobStatus.FAILED);
        job.setFinishedAt(Instant.now());
        job.setLastError(error);
        jobs.save(job);
        log.warn("Job {} FAILED: {}", jobId, error);
    }
    public void complete(UUID jobId, JobStatus status, String outputKey, String error) {
        if (status == null) throw new IllegalArgumentException("status is required");
        switch (status) {
            case RUNNING -> {
                // allow controller to mark as running if you expose that
                markStarted(jobId);
            }
            case DONE -> {
                // You can pass size/sha later if you compute them; nulls are fine here.
                completeSuccess(jobId, outputKey, null, null);
            }
            case FAILED -> {
                completeFailure(jobId, error != null ? error : "unknown error");
            }

            default -> throw new IllegalArgumentException("Unhandled status: " + status);
        }
    }
    private static String safe(String s){ return s==null ? "" : s; }
}
