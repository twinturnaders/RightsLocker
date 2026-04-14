package org.rights.locker.Controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rights.locker.DTOs.EvidenceDetailsDto;
import org.rights.locker.DTOs.FinalizeResponse;
import org.rights.locker.DTOs.MediaMetadata;
import org.rights.locker.DTOs.AuthenticityAssessment;
import org.rights.locker.Entities.AppUser;
import org.rights.locker.Entities.Evidence;
import org.rights.locker.Entities.ProcessingJob;
import org.rights.locker.Enums.CustodyEventType;
import org.rights.locker.Enums.EvidenceStatus;
import org.rights.locker.Enums.JobStatus;
import org.rights.locker.Enums.JobType;
import org.rights.locker.Repos.EvidenceRepo;
import org.rights.locker.Repos.MetadataService;
import org.rights.locker.Repos.ProcessingJobRepo;
import org.rights.locker.Security.UserPrincipal;
import org.rights.locker.Services.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import java.util.HashMap;
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
    private final AuthenticityAssessmentService authenticityAssessmentService;
    private final EvidenceService evidenceService;
    private final UserPrincipalService principalService;
    private final UserService userService;
    private final PDFBuilderService pdfBuilderService;

    @Value("${app.s3.bucketOriginals}") private String bucketOriginals;
    @Value("${app.s3.bucketHot}") private String bucketHot;

    @GetMapping
    public ResponseEntity<?> list(@AuthenticationPrincipal UserPrincipal principal,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {

        AppUser user = principalService.requireUser(principal);
        Pageable paging = PageRequest.of(page, size);
        return ResponseEntity.ok(evidenceService.list(user, paging));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        principalService.requireUser(principal);

        Evidence ev = evidenceRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return ResponseEntity.ok(evidenceService.getDetailsDTO(ev));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        principalService.requireUser(principal);
        AppUser user = userService.getAppUser(principal.getId());

        Evidence ev = evidenceRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Check ownership
        if (ev.getOwnerId() == null || !ev.getOwnerId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        if (ev.getOriginalKey() == null || ev.getOriginalKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Original media is not available yet.");
        }

        // Build metadata map for PDF generation
        Map<String, Object> metadata = buildMetadataMap(ev);
        byte[] pdfBytes = pdfBuilderService.buildMetadataPdf(metadata);

        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate PDF");
        }

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "inline; filename=\"evidence-" + ev.getId() + "-metadata.pdf\"")
                .body(pdfBytes);
    }

    @GetMapping("/{id}/pdf-url")
    public ResponseEntity<Map<String, String>> getPdfUrl(@PathVariable UUID id,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        principalService.requireUser(principal);
        AppUser user = userService.getAppUser(principal.getId());

        Evidence ev = evidenceRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Check ownership
        if (ev.getOwnerId() == null || !ev.getOwnerId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        String pdfUrl = "/api/evidence/" + id + "/pdf";
        return ResponseEntity.ok(Map.of("url", pdfUrl));
    }

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

    public record FinalizeReq(
            String key, String title, String description,
            String capturedAtIso, Double lat, Double lon, Double accuracy,
            String redactMode
    ){}

    @PostMapping("/finalize")
    public FinalizeResponse finalizeUpload(@RequestBody FinalizeReq req,
                                           @AuthenticationPrincipal UserPrincipal principal) throws Exception {

        String sha256; long size = 0;
        try (ResponseInputStream<GetObjectResponse> in = s3.getObject(b -> b.bucket(bucketOriginals).key(req.key()));
             InputStream is = in) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192]; int r;
            while ((r = is.read(buf)) != -1) { md.update(buf, 0, r); size += r; }
            sha256 = HexFormat.of().formatHex(md.digest());
        }

        var presigned = presigner.presignGetObject(b -> b
                .signatureDuration(java.time.Duration.ofMinutes(5))
                .getObjectRequest(r -> r.bucket(bucketOriginals).key(req.key())));
        String originalUrl = presigned.url().toString();

        MediaMetadata meta = null;
        try { meta = metadataService.extractFromUrl(originalUrl); }
        catch (Exception e){ log.warn("metadata extraction failed: {}", e.toString()); }

        AuthenticityAssessment authenticityAssessment = authenticityAssessmentService.assess(meta);
        if (meta != null) {
            meta = meta.withAuthenticityAssessment(authenticityAssessment);
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

        ev.setProvenanceStatus(authenticityAssessment.provenanceStatus());
        ev.setMetadataIntegrity(authenticityAssessment.metadataIntegrity());
        ev.setSyntheticMediaRisk(authenticityAssessment.syntheticMediaRisk());
        ev.setManipulationSignals(authenticityAssessment.manipulationSignals());
        ev.setAssessmentSummary(authenticityAssessment.assessmentSummary());

        String redactMode = (req.redactMode() == null || req.redactMode().isBlank())
                ? "BLUR" : req.redactMode().toUpperCase();

        AppUser currentUser = principalService.getUserOrNull(principal);
        ev.setOwner(currentUser);

        ev = evidenceRepo.save(ev);

        custody.record(ev, currentUser, CustodyEventType.RECEIVED, Map.of("source", "presigned"));

        String shareToken = null;
        if (currentUser == null) {
            var share = shareService.create(ev.getId(), Instant.now().plus(java.time.Duration.ofHours(24)), false);
            shareToken = share.getToken();
        }

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

    @GetMapping("/{id}/download")
    public Map<String,String> download(@PathVariable UUID id,
                                       @RequestParam(defaultValue = "original") String type,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        principalService.requireUser(principal);

        var ev = evidenceRepo.findById(id)
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

    @PostMapping("/{id}/legal-hold")
    @Transactional
    public ResponseEntity<EvidenceDetailsDto> setLegalHold(@PathVariable UUID id,
                                                           @RequestBody Map<String, Object> request,
                                                           @AuthenticationPrincipal UserPrincipal principal) {

        principalService.requireUser(principal);
        AppUser user = userService.getAppUser(principal.getId());

        boolean legalHold = Boolean.TRUE.equals(request.get("legalHold"));

        var ev = evidenceRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Check ownership
        var ownerId = ev.getOwner().getId();
        if (!ownerId.equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        ev.setLegalHold(legalHold);
        ev.setUpdatedAt(Instant.now());
        var custEvent = legalHold
                ? CustodyEventType.LEGAL_HOLD_ON
                : CustodyEventType.LEGAL_HOLD_OFF;

        evidenceRepo.save(ev);
        custody.record(ev, user, custEvent, Map.of());

        return ResponseEntity.ok(evidenceService.getDetailsDTO(ev));
    }

    @DeleteMapping("/{id}")
    public void deleteEvidence(@PathVariable UUID id,
                               @AuthenticationPrincipal UserPrincipal principal) {

        principalService.requireUser(principal);
        AppUser user = userService.getAppUser(principal.getId());
        var ev = evidenceRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

                        // Check ownership

                        if (ev.getOwnerId() == null || !ev.getOwnerId().equals(user.getId())) {
                            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                        }

        if (Boolean.TRUE.equals(ev.getLegalHold())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Evidence is on legal hold and cannot be deleted.");
        }

        try { custody.record(ev, user, CustodyEventType.DELETED, Map.of()); } catch (Exception ignore) {}

        jobRepo.deleteByEvidenceId(ev.getId());
        shareService.deleteByEvidenceId(ev.getId());

        deleteQuiet(bucketOriginals, ev.getOriginalKey());
        deleteQuiet(bucketHot, ev.getRedactedKey());
        deleteQuiet(bucketHot, ev.getThumbnailKey());
        deleteQuiet(bucketHot, ev.getDerivativeKey());

        evidenceRepo.delete(ev);
    }

    private Map<String, Object> buildMetadataMap(Evidence ev) {
        Map<String, Object> metadata = new HashMap<>();

        metadata.put("evidenceId", ev.getId().toString());
        metadata.put("title", ev.getTitle());
        metadata.put("description", ev.getDescription());
        metadata.put("capturedAt", ev.getCapturedAt());
        metadata.put("ingestedAt", ev.getCreatedAt());
        metadata.put("originalSha256", ev.getOriginalSha256());
        metadata.put("originalSizeB", ev.getOriginalSizeB());
        metadata.put("status", ev.getStatus());
        metadata.put("legalHold", ev.getLegalHold());

        // EXIF/metadata fields
        metadata.put("dateOriginal", ev.getExifDateOriginal());
        metadata.put("tzMinutes", ev.getTzOffsetMinutes());
        if (ev.getCaptureLatlon() != null) {
            metadata.put("lat", ev.getCaptureLatlon().getY());
            metadata.put("lon", ev.getCaptureLatlon().getX());
        }
        metadata.put("altitudeM", ev.getCaptureAltitudeM());
        metadata.put("headingDeg", ev.getCaptureHeadingDeg());
        metadata.put("cameraMake", ev.getCameraMake());
        metadata.put("cameraModel", ev.getCameraModel());
        metadata.put("lensModel", ev.getLensModel());
        metadata.put("software", ev.getSoftware());
        metadata.put("widthPx", ev.getWidthPx());
        metadata.put("heightPx", ev.getHeightPx());
        metadata.put("orientationDeg", ev.getOrientationDeg());

        // Video fields
        metadata.put("container", ev.getContainer());
        metadata.put("videoCodec", ev.getVideoCodec());
        metadata.put("audioCodec", ev.getAudioCodec());
        metadata.put("durationMs", ev.getDurationMs());
        metadata.put("videoFps", ev.getVideoFps());
        metadata.put("videoRotationDeg", ev.getVideoRotationDeg());

        // Authenticity assessment
        metadata.put("provenanceStatus", ev.getProvenanceStatus());
        metadata.put("metadataIntegrity", ev.getMetadataIntegrity());
        metadata.put("syntheticMediaRisk", ev.getSyntheticMediaRisk());
        metadata.put("manipulationSignals", ev.getManipulationSignals());
        metadata.put("assessmentSummary", ev.getAssessmentSummary());

        return metadata;
    }

    private void deleteQuiet(String bucket, String key) {
        if (key == null || key.isBlank()) return;
        try { s3.deleteObject(b -> b.bucket(bucket).key(key)); }
        catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException ignored) {}
        catch (Exception e) { log.warn("S3 delete failed for {}/{}: {}", bucket, key, e.toString()); }
    }

    private static Instant parseInstant(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try { return OffsetDateTime.parse(iso).toInstant(); }
        catch (DateTimeParseException ex) { return Instant.parse(iso); }
    }
}