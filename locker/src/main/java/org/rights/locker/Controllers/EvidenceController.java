package org.rights.locker.Controllers;

import lombok.RequiredArgsConstructor;
import org.rights.locker.Entities.Evidence;
import org.rights.locker.Entities.ProcessingJob;
import org.rights.locker.Enums.CustodyEventType;
import org.rights.locker.Enums.EvidenceStatus;
import org.rights.locker.Enums.JobStatus;
import org.rights.locker.Enums.JobType;
import org.rights.locker.Repos.EvidenceRepo;
import org.rights.locker.Repos.ProcessingJobRepo;
import org.rights.locker.Services.CustodyService;
import org.rights.locker.Services.ProcessorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/evidence")
@RequiredArgsConstructor
public class EvidenceController {

    private final S3Client s3;
    private final S3Presigner presigner;

    private final EvidenceRepo evidenceRepo;
    private final ProcessingJobRepo jobRepo;
    private final ProcessorService processor;
    private final CustodyService custody;

    @Value("${app.s3.bucketOriginals}") private String bucketOriginals;
    @Value("${app.s3.bucketHot}") private String bucketHot;

    /* -----------------------------
     *  List / Get
     * ----------------------------- */
    @GetMapping
    public Page<Evidence> list(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return evidenceRepo.findAll(PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public Evidence get(@PathVariable UUID id) {
        return evidenceRepo.findById(id).orElseThrow();
    }

    /* -----------------------------
     *  Presign upload (PUT to Originals)
     * ----------------------------- */
    public record PresignUploadReq(String filename, String contentType) {}
    @PostMapping("/presign-upload")
    public Map<String,Object> presignUpload(@org.springframework.web.bind.annotation.RequestBody PresignUploadReq req) {
        String safeName = (req.filename() == null ? "file.bin" : req.filename()).replaceAll("[^a-zA-Z0-9._-]", "_");
        String key = UUID.randomUUID() + "/original-" + safeName;

        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucketOriginals)
                .key(key)
                .contentType(req.contentType() == null ? "application/octet-stream" : req.contentType())
                .build();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(b -> b
                .signatureDuration(java.time.Duration.ofMinutes(15))
                .putObjectRequest(put));

        // Some browsers require Content-Type to match what we signed
        return Map.of(
                "key", key,
                "url", presigned.url().toString(),
                "headers", presigned.signedHeaders()
        );
    }

    /* -----------------------------
     *  Finalize upload (compute hash + size, create row, enqueue)
     * ----------------------------- */
    public record FinalizeReq(
            String key,
            String title,
            String description,
            String capturedAtIso,
            Double lat,
            Double lon,
            Double accuracy
    ) {}

    @PostMapping("/finalize")
    public Evidence finalizeUpload(@RequestBody FinalizeReq req) throws Exception {
        if (req.key() == null || req.key().isBlank()) throw new IllegalArgumentException("key is required");

        // Optional: HEAD for metadata/size (not strictly required; we’ll compute size anyway)
        HeadObjectResponse head = s3.headObject(HeadObjectRequest.builder()
                .bucket(bucketOriginals).key(req.key()).build());

        // Stream object to compute SHA-256 + size
        String sha256;
        long size = 0;
        try (ResponseInputStream<GetObjectResponse> in = s3.getObject(GetObjectRequest.builder()
                .bucket(bucketOriginals).key(req.key()).build());
             InputStream is = in) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int r;
            while ((r = is.read(buf)) != -1) {
                md.update(buf, 0, r);
                size += r;
            }
            sha256 = HexFormat.of().formatHex(md.digest());
        }

        // Build Evidence entity
        Instant capturedAt = parseInstant(req.capturedAtIso());
        var ev = Evidence.builder()
                .title(req.title())
                .description(req.description())
                .capturedAt(capturedAt)
                .originalKey(req.key())
                .originalSizeB(size)
                .originalSha256(sha256)
                .status(EvidenceStatus.RECEIVED)
                .legalHold(false)
                .build();

        // If you store geo as JTS Point, set it here (omit if not using spatial yet)
        // if (req.lat() != null && req.lon() != null) { ... }

        ev = evidenceRepo.save(ev);
        custody.record(ev, null, CustodyEventType.RECEIVED, "{\"source\":\"presigned\"}");

        // Enqueue processing jobs (thumbnail + redact)
        var thumb = jobRepo.save(ProcessingJob.builder()
                .evidence(ev).type(JobType.THUMBNAIL).status(JobStatus.QUEUED).attempts(0).build());
        processor.publish(thumb);

        var redact = jobRepo.save(ProcessingJob.builder()
                .evidence(ev).type(JobType.REDACT).status(JobStatus.QUEUED).attempts(0).build());
        processor.publish(redact);

        return ev;
    }

    private Instant parseInstant(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            // Accept both OffsetDateTime and Instant strings
            return OffsetDateTime.parse(iso).toInstant();
        } catch (DateTimeParseException ex) {
            return Instant.parse(iso);
        }
    }

    /* -----------------------------
     *  Download (presign GET; prefer hot derivative if present)
     * ----------------------------- */
    @GetMapping("/{id}/download")
    public Map<String,String> download(@PathVariable UUID id,
                                       @RequestParam(defaultValue = "redacted") String type) {
        var ev = evidenceRepo.findById(id).orElseThrow();

        String bucket; String key;

        switch (type.toLowerCase()) {
            case "thumbnail" -> {
                key = ev.getThumbnailKey();
                if (key == null) { // fallback: original
                    bucket = bucketOriginals; key = ev.getOriginalKey();
                } else {
                    bucket = bucketHot;
                }
            }
            case "original" -> {
                bucket = bucketOriginals;
                key = ev.getOriginalKey();
            }
            default /* "redacted" */ -> {
                key = ev.getRedactedKey();
                if (key == null) { // fallback: original
                    bucket = bucketOriginals; key = ev.getOriginalKey();
                } else {
                    bucket = bucketHot;
                }
            }
        }

        String finalKey = key;
        var presigned = presigner.presignGetObject(b -> b
                .signatureDuration(java.time.Duration.ofMinutes(10))
                .getObjectRequest(r -> r.bucket(bucket).key(finalKey)));

        return Map.of("url", presigned.url().toString());
    }
    /* -----------------------------
     *  Legal hold toggle
     * ----------------------------- */
    public record LegalHoldReq(boolean legalHold) {}
    @PostMapping("/{id}/legal-hold")
    public Evidence setLegalHold(@PathVariable  UUID id, @RequestBody LegalHoldReq body) {
        var ev = evidenceRepo.findById(id).orElseThrow();
        ev.setLegalHold(body.legalHold());
        ev = evidenceRepo.save(ev);
        custody.record(ev, null, body.legalHold() ? CustodyEventType.LEGAL_HOLD_ON : CustodyEventType.LEGAL_HOLD_OFF, "{}");
        return ev;
    }
}
