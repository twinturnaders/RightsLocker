package org.rights.locker.Controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rights.locker.Entities.Evidence;
import org.rights.locker.Entities.ShareLink;
import org.rights.locker.Repos.EvidenceRepo;
import org.rights.locker.Repos.ShareLinkRepo;
import org.rights.locker.Services.PDFBuilderService;
import org.rights.locker.Services.ShareService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;
    private final EvidenceRepo evidenceRepo;
    private final ShareLinkRepo shareLinkRepo;
    private final S3Client s3;
    private final S3Presigner presigner;
    private final ObjectMapper om;
    private final PDFBuilderService pdfService;

    @Value("${app.s3.bucketOriginals}") private String bucketOriginals;
    @Value("${app.s3.bucketHot}") private String bucketHot;

    @PostMapping("/evidence/{id}/share")
    public ShareLink create(@PathVariable UUID id, @RequestBody org.rights.locker.DTOs.ShareCreateRequest req){
        return shareService.create(id, req.expiresAt(), req.allowOriginal());
    }

    @GetMapping("/share/{token}")
    public ResponseEntity<?> getShare(@PathVariable String token){
        var share = shareService.requireActive(token);
        var ev = evidenceRepo.findById(share.getEvidenceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidence not found"));

        boolean hasRedacted = ev.getRedactedKey() != null && !ev.getRedactedKey().isBlank();
        boolean hasThumb    = ev.getThumbnailKey() != null && !ev.getThumbnailKey().isBlank();

        var redactedUrl = hasRedacted ? presign(bucketHot,        ev.getRedactedKey(),  Duration.ofMinutes(10)) : null;
        var originalUrl = share.isAllowOriginal() ? presign(bucketOriginals, ev.getOriginalKey(), Duration.ofMinutes(10)) : null;
        var thumbUrl    = hasThumb    ? presign(bucketHot,        ev.getThumbnailKey(), Duration.ofMinutes(10)) : null;

        var evidenceMap = new LinkedHashMap<String,Object>();
        evidenceMap.put("id", ev.getId());
        evidenceMap.put("title", ev.getTitle());
        evidenceMap.put("description", ev.getDescription());
        evidenceMap.put("capturedAt", ev.getCapturedAt());
        evidenceMap.put("status", ev.getStatus() == null ? null : ev.getStatus().name());
        evidenceMap.put("hasRedacted", hasRedacted);
        evidenceMap.put("hasThumb", hasThumb);

        // expose a subset of rich metadata (safe for viewers)
        evidenceMap.put("exifDateOriginal", ev.getExifDateOriginal());
        evidenceMap.put("cameraMake", ev.getCameraMake());
        evidenceMap.put("cameraModel", ev.getCameraModel());
        evidenceMap.put("widthPx", ev.getWidthPx());
        evidenceMap.put("heightPx", ev.getHeightPx());
        evidenceMap.put("container", ev.getContainer());
        evidenceMap.put("videoCodec", ev.getVideoCodec());
        evidenceMap.put("audioCodec", ev.getAudioCodec());
        evidenceMap.put("durationMs", ev.getDurationMs());
        evidenceMap.put("videoFps", ev.getVideoFps());

        var linksMap = new LinkedHashMap<String,Object>();
        if (redactedUrl != null) linksMap.put("redactedUrl", redactedUrl);
        if (originalUrl != null) linksMap.put("originalUrl", originalUrl);
        if (thumbUrl != null)    linksMap.put("thumbUrl",    thumbUrl);

        var payload = new LinkedHashMap<String,Object>();
        payload.put("token", share.getToken());
        payload.put("expiresAt", share.getExpiresAt());
        payload.put("allowOriginal", share.isAllowOriginal());
        payload.put("evidence", evidenceMap);
        payload.put("links", linksMap);

        return ResponseEntity.ok(payload);
    }

    @GetMapping(value = "/share/{token}/metadata.pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> shareMetadataPdf(@PathVariable String token) {
        var s  = shareService.requireActive(token);
        var ev = evidenceRepo.findById(s.getEvidenceId()).orElseThrow();

        var m = new LinkedHashMap<String,Object>();
        m.put("evidenceId", ev.getId().toString());
        m.put("title", ev.getTitle());
        m.put("description", ev.getDescription());
        m.put("capturedAt", ev.getCapturedAt() == null ? null : DateTimeFormatter.ISO_INSTANT.format(ev.getCapturedAt()));
        m.put("ingestedAt", ev.getCreatedAt() == null ? null : DateTimeFormatter.ISO_INSTANT.format(ev.getCreatedAt()));
        m.put("generatedAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        m.put("hashAlgorithm", "SHA-256");
        m.put("originalSha256", ev.getOriginalSha256());
        m.put("originalSizeB", ev.getOriginalSizeB());

        // rich metadata
        m.put("dateOriginal", ev.getExifDateOriginal());
        m.put("tzMinutes", ev.getTzOffsetMinutes());
        m.put("altitudeM", ev.getCaptureAltitudeM());
        m.put("headingDeg", ev.getCaptureHeadingDeg());
        m.put("cameraMake", ev.getCameraMake());
        m.put("cameraModel", ev.getCameraModel());
        m.put("lensModel", ev.getLensModel());
        m.put("software", ev.getSoftware());
        m.put("widthPx", ev.getWidthPx());
        m.put("heightPx", ev.getHeightPx());
        m.put("orientationDeg", ev.getOrientationDeg());
        m.put("container", ev.getContainer());
        m.put("videoCodec", ev.getVideoCodec());
        m.put("audioCodec", ev.getAudioCodec());
        m.put("durationMs", ev.getDurationMs());
        m.put("videoFps", ev.getVideoFps());
        m.put("videoRotationDeg", ev.getVideoRotationDeg());

        // share context
        m.put("shareToken", s.getToken());
        m.put("shareAllowOriginal", s.isAllowOriginal());
        m.put("shareExpiresAt", s.getExpiresAt());

        byte[] pdf = pdfService.buildMetadataPdf(m);
        return ResponseEntity.ok()
                .header("Content-Disposition","inline; filename=\"metadata.pdf\"")
                .body(pdf);
    }

    /* zipped package (same logic as before, trimmed here to show bucket switch + pdf include) */
    @GetMapping("/share/{token}/package")
    public ResponseEntity<StreamingResponseBody> downloadSharedPackage(
            @PathVariable String token,
            @RequestParam(defaultValue = "redacted") String type
    ) {
        var link = shareLinkRepo.findByToken(token).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var ev = evidenceRepo.findById(link.getEvidenceId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String chosenKey = switch (type.toLowerCase()) {
            case "redacted" -> (ev.getRedactedKey() != null ? ev.getRedactedKey() : ev.getOriginalKey());
            case "original" -> ev.getOriginalKey();
            default -> ev.getRedactedKey();
        };
        if (chosenKey == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No asset available to package");

        String chosenBucket = chosenKey.equals(ev.getOriginalKey()) ? bucketOriginals : bucketHot;

        StreamingResponseBody body = out -> {
            try (var zos = new java.util.zip.ZipOutputStream(out)) {
                // manifest
                var manifest = new LinkedHashMap<String,Object>();
                manifest.put("evidenceId", ev.getId().toString());
                manifest.put("title", ev.getTitle());
                manifest.put("description", ev.getDescription());
                manifest.put("capturedAt", ev.getCapturedAt());
                manifest.put("status", ev.getStatus() == null ? null : ev.getStatus().name());
                manifest.put("includedObjectKey", chosenKey);
                zos.putNextEntry(new java.util.zip.ZipEntry("manifest.json"));
                zos.write(om.writeValueAsBytes(manifest));
                zos.closeEntry();

                // media
                zos.putNextEntry(new java.util.zip.ZipEntry(filenameForKey(chosenKey)));
                s3.getObject(b -> b.bucket(chosenBucket).key(chosenKey)).transferTo(zos);
                zos.closeEntry();

                // pdf
                byte[] pdf = sharePdfMap(ev, link);
                if (pdf != null && pdf.length > 0) {
                    zos.putNextEntry(new ZipEntry("metadata.pdf"));
                    zos.write(pdf);
                    zos.closeEntry();
                }

                zos.finish();
            }
        };

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"evidence-" + ev.getId() + ".zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }

    /* helpers */
    private byte[] sharePdfMap(Evidence ev, ShareLink s){
        var m = new LinkedHashMap<String,Object>();
        m.put("evidenceId", ev.getId().toString());
        m.put("title", ev.getTitle());
        m.put("description", ev.getDescription());
        m.put("capturedAt", ev.getCapturedAt() == null ? null : DateTimeFormatter.ISO_INSTANT.format(ev.getCapturedAt()));
        m.put("ingestedAt", ev.getCreatedAt() == null ? null : DateTimeFormatter.ISO_INSTANT.format(ev.getCreatedAt()));
        m.put("generatedAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        m.put("hashAlgorithm", "SHA-256");
        m.put("originalSha256", ev.getOriginalSha256());
        m.put("originalSizeB", ev.getOriginalSizeB());
        // rich
        m.put("dateOriginal", ev.getExifDateOriginal());
        m.put("tzMinutes", ev.getTzOffsetMinutes());
        m.put("altitudeM", ev.getCaptureAltitudeM());
        m.put("headingDeg", ev.getCaptureHeadingDeg());
        m.put("cameraMake", ev.getCameraMake());
        m.put("cameraModel", ev.getCameraModel());
        m.put("lensModel", ev.getLensModel());
        m.put("software", ev.getSoftware());
        m.put("widthPx", ev.getWidthPx());
        m.put("heightPx", ev.getHeightPx());
        m.put("orientationDeg", ev.getOrientationDeg());
        m.put("container", ev.getContainer());
        m.put("videoCodec", ev.getVideoCodec());
        m.put("audioCodec", ev.getAudioCodec());
        m.put("durationMs", ev.getDurationMs());
        m.put("videoFps", ev.getVideoFps());
        m.put("videoRotationDeg", ev.getVideoRotationDeg());
        // share
        m.put("shareToken", s.getToken());
        m.put("shareAllowOriginal", s.isAllowOriginal());
        m.put("shareExpiresAt", s.getExpiresAt());

        try { return pdfService.buildMetadataPdf(m); }
        catch (Exception e){ log.warn("PDF build failed: {}", e.toString()); return new byte[0]; }
    }

    private String presign(String bucket, String key, Duration ttl) {
        if (key == null || key.isBlank()) return null;
        var req = GetObjectRequest.builder().bucket(bucket).key(key).build();
        return presigner.presignGetObject(b -> b.signatureDuration(ttl).getObjectRequest(req)).url().toString();
    }
    private static String filenameForKey(String key) {
        int i = key.lastIndexOf('/');
        return i >= 0 ? key.substring(i + 1) : key;
    }
}
