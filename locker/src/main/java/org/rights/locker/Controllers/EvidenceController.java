package org.rights.locker.Controllers;

import lombok.RequiredArgsConstructor;
import org.rights.locker.DTOs.FinalizeResponse;
import org.rights.locker.Entities.AppUser;
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
import org.rights.locker.Services.ShareService;
import org.rights.locker.Services.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

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
    private final StorageService storage;
    private final ShareService shareService;

    @Value("${app.s3.bucketOriginals}") private String bucketOriginals;
    @Value("${app.s3.bucketHot}") private String bucketHot;

    /* --------- List/Get (owner-scoped) --------- */

    @GetMapping
    public Page<Evidence> list(@RequestParam(defaultValue="0") int page,
                               @RequestParam(defaultValue="20") int size,
                               Authentication auth) {
        AppUser current = (auth != null && auth.getPrincipal() instanceof AppUser u) ? u : null;
        if (current == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return evidenceRepo.findAllByOwner(current, PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public Evidence get(@PathVariable UUID id, Authentication auth) {
        AppUser current = (auth != null && auth.getPrincipal() instanceof AppUser u) ? u : null;
        if (current == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return evidenceRepo.findByIdAndOwner(id, current)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /* --------- Upload: presign PUT to Originals --------- */

    public record PresignUploadReq(String filename, String contentType) {}

    @PostMapping("/presign-upload")
    public Map<String, Object> presignUpload(@RequestBody PresignUploadReq req) {
        String safeName = (req.filename() == null ? "file.bin" : req.filename())
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        String key = UUID.randomUUID() + "/original-" + safeName;

        Map<String, Object> signed = storage.signedPut(
                key,
                bucketOriginals,
                15 * 60, // 15 minutes
                (req.contentType() == null ? "application/octet-stream" : req.contentType())
        );

        return new java.util.HashMap<>() {{
            putAll(signed);
            put("key", key);
        }};
    }

    /* --------- Finalize: compute hash/size, create row, enqueue --------- */

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
    public FinalizeResponse finalizeUpload(@RequestBody FinalizeReq req,
                                           @AuthenticationPrincipal AppUser currentUser) throws Exception {
        if (req.key() == null || req.key().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "key is required");
        }

        // Optional: HEAD (exists/metadata)
        HeadObjectResponse head = s3.headObject(HeadObjectRequest.builder()
                .bucket(bucketOriginals).key(req.key()).build());

        // Stream to compute SHA-256 + size
        String sha256;
        long size = 0L;
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

        // Build + SAVE the Evidence (always save to get UUID)
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
                .owner(currentUser) // null if anonymous — allowed
                .build();

        ev = evidenceRepo.save(ev);

        // Chain-of-custody
        custody.record(ev, currentUser, CustodyEventType.RECEIVED, Map.of("source", "presigned"));

        // If anonymous, mint 24h share token so the browser has a way back
        String shareToken = null;
        if (currentUser == null) {
            var share = shareService.create(ev.getId(), Instant.now().plus(java.time.Duration.ofHours(24)), false);
            shareToken = share.getToken();
        }

        // Enqueue processing (thumb + redact)
        var thumb = jobRepo.save(ProcessingJob.builder()
                .evidence(ev).type(JobType.THUMBNAIL).status(JobStatus.QUEUED).attempts(0).build());
        processor.publish(thumb);

        var redact = jobRepo.save(ProcessingJob.builder()
                .evidence(ev).type(JobType.REDACT).status(JobStatus.QUEUED).attempts(0).build());
        processor.publish(redact);

        return new FinalizeResponse(ev, shareToken);
    }

    private Instant parseInstant(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return OffsetDateTime.parse(iso).toInstant();
        } catch (DateTimeParseException ex) {
            return Instant.parse(iso);
        }
    }

    @GetMapping("/{id}/download")
    public Map<String,String> download(@PathVariable UUID id,
                                       @RequestParam(defaultValue = "redacted") String type) {
        var ev = evidenceRepo.findById(id).orElseThrow();

        String bucket; String key;
        switch (type.toLowerCase()) {
            case "thumbnail" -> {
                key = ev.getThumbnailKey();
                if (key == null) { bucket = bucketOriginals; key = ev.getOriginalKey(); }
                else { bucket = bucketHot; }
            }
            case "original" -> { bucket = bucketOriginals; key = ev.getOriginalKey(); }
            default -> { // redacted
                key = ev.getRedactedKey();
                if (key == null) { bucket = bucketOriginals; key = ev.getOriginalKey(); }
                else { bucket = bucketHot; }
            }
        }
        String finalKey = key;
        var presigned = presigner.presignGetObject(b -> b
                .signatureDuration(java.time.Duration.ofMinutes(10))
                .getObjectRequest(r -> r.bucket(bucket).key(finalKey)));
        return Map.of("url", presigned.url().toString());
    }

    public record LegalHoldReq(boolean legalHold) {}

    @PostMapping("/{id}/legal-hold")
    public Evidence setLegalHold(@PathVariable UUID id,
                                 @RequestBody LegalHoldReq body,
                                 @AuthenticationPrincipal AppUser current) {
        if (current == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var ev = evidenceRepo.findByIdAndOwner(id, current)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ev.setLegalHold(body.legalHold());
        ev = evidenceRepo.save(ev);
        custody.record(ev, current, body.legalHold() ? CustodyEventType.LEGAL_HOLD_ON : CustodyEventType.LEGAL_HOLD_OFF,
                Map.of("source", "ui"));
        return ev;
    }
}
