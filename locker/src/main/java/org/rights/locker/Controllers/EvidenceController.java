package org.rights.locker.Controllers;

import io.jsonwebtoken.Jwt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.index.hprtree.Item;
import org.rights.locker.DTOs.FinalizeResponse;
import org.rights.locker.DTOs.MediaMetadata;
import org.rights.locker.Entities.AppUser;
import org.rights.locker.Entities.Evidence;
import org.rights.locker.Entities.ProcessingJob;
import org.rights.locker.Enums.CustodyEventType;
import org.rights.locker.Enums.EvidenceStatus;
import org.rights.locker.Enums.JobStatus;
import org.rights.locker.Enums.JobType;

import org.rights.locker.Repos.AppUserRepo;
import org.rights.locker.Repos.EvidenceRepo;
import org.rights.locker.Repos.MetadataService;
import org.rights.locker.Repos.ProcessingJobRepo;
import org.rights.locker.Security.UserPrincipal;
import org.rights.locker.Services.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;



@Slf4j
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
  private final MetadataService metadataService;
  private final AppUserRepo appUserRepo;
    private final EvidenceService evidenceService;

    @Value("${app.s3.bucketOriginals}") private String bucketOriginals;
    @Value("${app.s3.bucketHot}") private String bucketHot;


    @GetMapping
    public ResponseEntity<?> list(@AuthenticationPrincipal AppUser user,
                                  @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size ){
//    @RequestParam(required = false) String searchTerm){


        // For future iterations for search of evidence
//        if (searchTerm != null && !searchTerm.isEmpty()) {
//            itemsPage = evidenceService.findItemsBySearchTerm(searchTerm, paging);

        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        Pageable paging = PageRequest.of(page, size);
        return ResponseEntity.ok(evidenceService.list(user, paging));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Evidence> get(@PathVariable UUID id, @AuthenticationPrincipal AppUser user) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
       return evidenceRepo.findById(id)
                .map(evidence -> new ResponseEntity<>(evidence, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));



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
            String capturedAtIso, Double lat, Double lon, Double accuracy,
            String redactMode
    ){}

    @PostMapping("/finalize")
    public FinalizeResponse finalizeUpload(@RequestBody FinalizeReq req,
                                           @AuthenticationPrincipal AppUser currentUser) throws Exception {

        // SHA-256 + size
        String sha256; long size = 0;
        try (ResponseInputStream<GetObjectResponse> in = s3.getObject(b -> b.bucket(bucketOriginals).key(req.key()));
             InputStream is = in) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192]; int r;
            while ((r = is.read(buf)) != -1) { md.update(buf, 0, r); size += r; }
            sha256 = HexFormat.of().formatHex(md.digest());
        }

        // Presign so extractors can read
        var presigned = presigner.presignGetObject(b -> b
                .signatureDuration(java.time.Duration.ofMinutes(5))
                .getObjectRequest(r -> r.bucket(bucketOriginals).key(req.key())));
        String originalUrl = presigned.url().toString();

        // Extract rich metadata
        MediaMetadata meta = null;
        try { meta = metadataService.extractFromUrl(originalUrl); }
        catch (Exception e){ log.warn("metadata extraction failed: {}", e.toString()); }

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

        // Optional: store client geo in your existing fields if you want
        // ev.setCaptureLatlon(...); ev.setCaptureAccuracyM(req.accuracy());

        // Persist extracted metadata if present
        if (meta != null) {
            ev.setExifDateOriginal(meta.dateOriginal());
            ev.setTzOffsetMinutes(meta.tzMinutes());
            ev.setCaptureAltitudeM(meta.altitudeM());
            ev.setCaptureHeadingDeg(meta.headingDeg());
            ev.setCameraMake(meta.cameraMake());
            ev.setCameraModel(meta.cameraModel());
            ev.setLensModel(meta.lensModel());
            ev.setSoftware(meta.software());
            ev.setWidthPx(meta.widthPx());
            ev.setHeightPx(meta.heightPx());
            ev.setOrientationDeg(meta.orientationDeg());
            ev.setContainer(meta.container());
            ev.setVideoCodec(meta.videoCodec());
            ev.setAudioCodec(meta.audioCodec());
            ev.setDurationMs(meta.durationMs());
            ev.setVideoFps(meta.videoFps());
            ev.setVideoRotationDeg(meta.videoRotationDeg());
        }

        String redactMode = (req.redactMode() == null || req.redactMode().isBlank())
                ? "BLUR" : req.redactMode().toUpperCase();

        if (currentUser != null) {
            ev.setOwner(currentUser);
        }
        ev = evidenceRepo.save(ev);

        custody.record(ev, currentUser, CustodyEventType.RECEIVED, Map.of("source", "presigned"));

        // If anonymous, mint a 24h share link (original disabled)
        String shareToken = null;
        if (currentUser == null) {
            var share = shareService.create(ev.getId(), Instant.now().plus(java.time.Duration.ofHours(24)), false);
            shareToken = share.getToken();
        }

        // Enqueue processing
        var thumb = jobRepo.save(ProcessingJob.builder()
                .evidence(ev).type(JobType.THUMBNAIL).status(JobStatus.QUEUED).attempts(0).build());
        processor.publish(thumb);

        var redact = jobRepo.save(ProcessingJob.builder()
                .evidence(ev)
                .type(JobType.REDACT)
                .status(JobStatus.QUEUED)
                .attempts(0)
                .payloadJson(Map.of("mode", redactMode))
                .build());
        processor.publish(redact);

        return new FinalizeResponse(ev, shareToken);
    }

    /* presigned GET per type (auth only) */
    @GetMapping("/{id}/download")
    public Map<String,String> download(@PathVariable UUID id,
                                       @RequestParam(defaultValue = "original") String type,
                                       @AuthenticationPrincipal AppUser user) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var ev = evidenceRepo.findByIdAndOwner(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String bucket; String key;
        switch (type.toLowerCase()) {
            case "thumbnail" -> {
                key = ev.getThumbnailKey();
                bucket = (key == null) ? bucketOriginals : bucketHot;
                if (key == null) key = ev.getOriginalKey();
            }
            case "redacted" -> { bucket = bucketHot; key = ev.getRedactedKey(); }
            default -> { bucket = bucketOriginals; key = ev.getOriginalKey(); }
        }
        if (key == null || key.isBlank()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        String finalKey = key;
        var presigned = presigner.presignGetObject(b -> b
                .signatureDuration(java.time.Duration.ofMinutes(10))
                .getObjectRequest(r -> r.bucket(bucket).key(finalKey)));
        return Map.of("url", presigned.url().toString());
    }

    /* legal hold toggle (auth only) */

    @PostMapping("/{id}/{legalHold}")
    @Transactional
    public ResponseEntity<Void> setLegalHold(@PathVariable UUID id,
                                             @PathVariable boolean legalHold,
                                             @AuthenticationPrincipal Jwt principal) {



        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);


        var ev = evidenceRepo.findByIdAndOwner(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        ev.setLegalHold(legalHold);
        ev.setUpdatedAt(Instant.now());
        var custEvent = CustodyEventType.LEGAL_HOLD_ON;
        if (!legalHold) {
            custEvent = CustodyEventType.LEGAL_HOLD_OFF;

        }
        evidenceRepo.save(ev);
        custody.record(
                ev,
                user,
                custEvent,
                Map.of()
        );

        // 204 avoids any Jackson serialization issues
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/{id}")
    public void deleteEvidence(@PathVariable UUID id, @AuthenticationPrincipal AppUser user) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var ev = evidenceRepo.findByIdAndOwner(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (ev.getLegalHold() != false) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Evidence is on legal hold and cannot be deleted.");
        }

        try { custody.record(ev, user, CustodyEventType.DELETED, Map.of()); } catch (Exception ignore) {}


        jobRepo.deleteByEvidenceId(ev.getId());
        shareService.deleteByEvidenceId(ev.getId());

        // delete S3 blobs (ignore missing keys)
        deleteQuiet(bucketOriginals, ev.getOriginalKey());
        deleteQuiet(bucketHot, ev.getRedactedKey());
        deleteQuiet(bucketHot, ev.getThumbnailKey());
        deleteQuiet(bucketHot, ev.getDerivativeKey());

        // finally remove the evidence row
        evidenceRepo.delete(ev);
    }

    private void deleteQuiet(String bucket, String key) {
        if (key == null || key.isBlank()) return;
        try { s3.deleteObject(b -> b.bucket(bucket).key(key)); }
        catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException ignored) {}
        catch (Exception e) { log.warn("S3 delete failed for {}/{}: {}", bucket, key, e.toString()); }
    }

    /* helpers */
    private static Instant parseInstant(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try { return OffsetDateTime.parse(iso).toInstant(); }
        catch (DateTimeParseException ex) { return Instant.parse(iso); }
    }
}
