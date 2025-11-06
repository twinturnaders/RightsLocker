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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
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

    /* auth-only list */
    @GetMapping
    public Page<Evidence> list(@RequestParam(defaultValue="0") int page,
                               @RequestParam(defaultValue="20") int size,
                               @AuthenticationPrincipal AppUser current) {
        if (current == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return evidenceRepo.findAllByOwner(current, PageRequest.of(page, size));
    }

    /* auth-only get */
    @GetMapping("/{id}")
    public Evidence get(@PathVariable UUID id, @AuthenticationPrincipal AppUser current) {
        if (current == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return evidenceRepo.findByIdAndOwner(id, current)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /* presign upload (public) */
    public record PresignUploadReq(String filename, String contentType) {}
    @PostMapping("/presign-upload")
    public Map<String, Object> presignUpload(@RequestBody PresignUploadReq req) {
        String safeName = (req.filename() == null ? "file.bin" : req.filename())
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        String key = java.util.UUID.randomUUID() + "/original-" + safeName;

        Map<String, Object> signed = storage.signedPut(
                key,
                bucketOriginals,
                15 * 60,
                (req.contentType() == null ? "application/octet-stream" : req.contentType())
        );

        return new java.util.HashMap<>() {{
            putAll(signed);
            put("key", key);
        }};
    }

    /* finalize (public) */
    public record FinalizeReq(
            String key, String title, String description,
            String capturedAtIso, Double lat, Double lon, Double accuracy
    ) {}

    @PostMapping("/finalize")
    public FinalizeResponse finalizeUpload(@RequestBody FinalizeReq req,
                                           @AuthenticationPrincipal AppUser currentUser) throws Exception {

        // compute SHA-256 + size from object
        String sha256; long size = 0;
        try (ResponseInputStream<GetObjectResponse> in = s3.getObject(b -> b.bucket(bucketOriginals).key(req.key()));
             InputStream is = in) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192]; int r;
            while ((r = is.read(buf)) != -1) { md.update(buf, 0, r); size += r; }
            sha256 = HexFormat.of().formatHex(md.digest());
        }

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

        if (currentUser != null) {
            ev.setOwner(currentUser);
        }
        ev = evidenceRepo.save(ev);

        custody.record(ev, currentUser, CustodyEventType.RECEIVED, Map.of("source", "presigned"));

        // if anonymous, mint a share token valid 24h for redacted/original (original not allowed by default)
        String shareToken = null;
        if (currentUser == null) {
            var share = shareService.create(ev.getId(), Instant.now().plus(java.time.Duration.ofHours(24)), false);
            shareToken = share.getToken();
        }

        // enqueue processing (thumb + redact)
        var thumb = jobRepo.save(ProcessingJob.builder()
                .evidence(ev).type(JobType.THUMBNAIL).status(JobStatus.QUEUED).attempts(0).build());
        processor.publish(thumb);

        var redact = jobRepo.save(ProcessingJob.builder()
                .evidence(ev).type(JobType.REDACT).status(JobStatus.QUEUED).attempts(0).build());
        processor.publish(redact);

        return new FinalizeResponse(ev, shareToken);
    }

    /* presigned GET per type (auth only) */
    @GetMapping("/{id}/download")
    public Map<String,String> download(@PathVariable UUID id,
                                       @RequestParam(defaultValue = "redacted") String type,
                                       @AuthenticationPrincipal AppUser current) {
        if (current == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var ev = evidenceRepo.findByIdAndOwner(id, current)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String bucket; String key;
        switch (type.toLowerCase()) {
            case "thumbnail" -> {
                key = ev.getThumbnailKey();
                bucket = (key == null) ? bucketOriginals : bucketHot;
                if (key == null) key = ev.getOriginalKey();
            }
            case "original" -> { bucket = bucketOriginals; key = ev.getOriginalKey(); }
            default /* redacted */ -> {
                key = ev.getRedactedKey();
                bucket = (key == null) ? bucketOriginals : bucketHot;
                if (key == null) key = ev.getOriginalKey();
            }
        }
        if (key == null || key.isBlank()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        String finalKey = key;
        var presigned = presigner.presignGetObject(b -> b
                .signatureDuration(java.time.Duration.ofMinutes(10))
                .getObjectRequest(r -> r.bucket(bucket).key(finalKey)));
        return Map.of("url", presigned.url().toString());
    }

    /* legal hold toggle (auth only) */
    public record LegalHoldReq(boolean legalHold) {}
    @PostMapping("/{id}/legal-hold")
    public Evidence setLegalHold(@PathVariable UUID id,
                                 @RequestBody LegalHoldReq body,
                                 @AuthenticationPrincipal AppUser current) {
        if (current == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var ev = evidenceRepo.findByIdAndOwner(id, current).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ev.setLegalHold(body.legalHold());
        ev = evidenceRepo.save(ev);
        custody.record(ev, current, body.legalHold()
                ? CustodyEventType.LEGAL_HOLD_ON : CustodyEventType.LEGAL_HOLD_OFF, Map.of());
        return ev;
    }

    private static Instant parseInstant(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try { return OffsetDateTime.parse(iso).toInstant(); }
        catch (DateTimeParseException ex) { return Instant.parse(iso); }
    }
}
