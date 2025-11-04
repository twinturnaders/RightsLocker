package org.rights.locker.Controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rights.locker.Entities.Evidence;
import org.rights.locker.Entities.ShareLink;
import org.rights.locker.Enums.EvidenceStatus;
import org.rights.locker.Repos.EvidenceRepo;
import org.rights.locker.Services.PDFBuilderService;
import org.rights.locker.Services.ShareService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
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
    private final S3Client s3;
    private final S3Presigner presigner;
    private final ObjectMapper om;
    private final PDFBuilderService pdfService;

    @Value("${app.s3.bucketOriginals}") private String bucketOriginals;
    @Value("${app.s3.bucketHot}") private String bucketHot;

    /* -------------------------------------------
     * Owner (or system) creates a capability link
     * ------------------------------------------- */
    @PostMapping("/evidence/{id}/share")
    public ShareLink create(@PathVariable UUID id, @RequestBody org.rights.locker.DTOs.ShareCreateRequest req){
        return shareService.create(id, req.expiresAt(), req.allowOriginal());
    }

    /* ---------------------------------------------------------
     * Public read: minimal metadata + short-lived presigned URLs
     * --------------------------------------------------------- */
    @GetMapping("/share/{token}")
    public ResponseEntity<?> getShare(@PathVariable String token){
        var share = shareService.requireActive(token); // throws if missing/expired/revoked
        var ev = evidenceRepo.findById(share.getEvidenceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidence not found"));

        boolean hasRedacted = ev.getRedactedKey() != null;
        boolean hasThumb = ev.getThumbnailKey() != null;

        // build short-lived (10 min) URLs
        var redactedUrl = hasRedacted ? presign(bucketHot, ev.getRedactedKey(), Duration.ofMinutes(10)) : null;
        var originalUrl = share.isAllowOriginal() ? presign(bucketOriginals, ev.getOriginalKey(), Duration.ofMinutes(10)) : null;
        var thumbUrl    = hasThumb ? presign(bucketHot, ev.getThumbnailKey(), Duration.ofMinutes(10)) : null;

        var payload = Map.of(
                "token", share.getToken(),
                "expiresAt", share.getExpiresAt(),
                "allowOriginal", share.isAllowOriginal(),
                "evidence", Map.of(
                        "id", ev.getId(),
                        "title", ev.getTitle(),
                        "description", ev.getDescription(),
                        "capturedAt", ev.getCapturedAt(),
                        "status", ev.getStatus(),
                        "hasRedacted", hasRedacted,
                        "hasThumb", hasThumb
                ),
                "links", Map.of(
                        "redactedUrl", redactedUrl,
                        "originalUrl", originalUrl,
                        "thumbUrl", thumbUrl
                )
        );

        return ResponseEntity.ok(payload);
    }

    @GetMapping(value = "/share/{token}/metadata.pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> getShareMetadataPdf(@PathVariable String token) throws Exception {
        var s = shareService.requireActive(token);
        var ev = evidenceRepo.findById(s.getEvidenceId()).orElseThrow();

        // Build the same metadata map you use in the ZIP:
        boolean isRedacted = ev.getRedactedKey() != null && !s.isAllowOriginal();
        var metadata = java.util.Map.of(
                "evidenceId", ev.getId().toString(),
                "title", ev.getTitle(),
                "capturedAt", ev.getCapturedAt() != null ? java.time.format.DateTimeFormatter.ISO_INSTANT.format(ev.getCapturedAt()) : null,
                "ingestedAt", ev.getCreatedAt() != null ? java.time.format.DateTimeFormatter.ISO_INSTANT.format(ev.getCreatedAt()) : null,
                "generatedAt", java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now()),
                "access", java.util.Map.of(
                        "viaShareToken", s.getToken(),
                        "allowOriginal", s.isAllowOriginal(),
                        "mode", isRedacted ? "redacted" : "original-or-redacted"
                )
        );

        // Reuse the same PDF builder (factor it into a small @Component if you prefer):
        byte[] pdf = pdfService.buildMetadataPdf(metadata);

        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"metadata.pdf\"")
                .body(pdf);
    }

    @GetMapping("/share/{token}/package")
    public ResponseEntity<StreamingResponseBody> downloadSharedPackage(@PathVariable String token,
                                                                       @RequestParam(defaultValue = "redacted") String type) {
        var share = shareService.requireActive(token);
        var ev = evidenceRepo.findById(share.getEvidenceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidence not found"));

        // Decide which object to deliver:
        final boolean wantOriginal = "original".equalsIgnoreCase(type);
        final boolean useOriginal = wantOriginal && share.isAllowOriginal();
        final boolean hasRedacted = ev.getRedactedKey() != null;

        final String bucket = useOriginal ? bucketOriginals : (hasRedacted ? bucketHot : bucketOriginals);
        final String key    = useOriginal ? ev.getOriginalKey() : (hasRedacted ? ev.getRedactedKey() : ev.getOriginalKey());
        final String filenameBase = useOriginal || !hasRedacted ? "original" : "redacted";

        if (key == null || key.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not available");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("evidence-" + ev.getId() + ".zip")
                .build());

        StreamingResponseBody body = out -> {
            try (var zip = new ZipOutputStream(out)) {

                // 1) media
                try (ResponseInputStream<GetObjectResponse> in =
                             s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())) {
                    var md = MessageDigest.getInstance("SHA-256");
                    long size = copyToZip(zip, in, filenameBase + guessExt(key), md);
                    String sha = HexFormat.of().formatHex(md.digest());

                    // 2) optional thumb
                    String thumbSha = null;
                    if (ev.getThumbnailKey() != null) {
                        try (ResponseInputStream<GetObjectResponse> th =
                                     s3.getObject(GetObjectRequest.builder().bucket(bucketHot).key(ev.getThumbnailKey()).build())) {
                            var tmd = MessageDigest.getInstance("SHA-256");
                            copyToZip(zip, th, "thumb.jpg", tmd);
                            thumbSha = HexFormat.of().formatHex(tmd.digest());
                        } catch (Exception ignore) { /* best-effort */ }
                    }

                    // 3) metadata.json (minimal public set)
                    var meta = Map.of(
                            "evidenceId", ev.getId().toString(),
                            "title", ev.getTitle(),
                            "capturedAt", ev.getCapturedAt(),
                            "deliveredKind", filenameBase,
                            "deliveredSha256", sha,
                            "deliveredSizeBytes", size
                    );
                    putText(zip, "metadata.json", om.writerWithDefaultPrettyPrinter().writeValueAsString(meta));

                    // 4) integrity.txt (simple public version)
                    var sb = new StringBuilder();
                    sb.append("RightsLocker Public Package\n");
                    sb.append("Evidence ID: ").append(ev.getId()).append("\n");
                    sb.append("Delivered: ").append(filenameBase).append("\n");
                    sb.append("SHA-256: ").append(sha).append("\n");
                    if (thumbSha != null) sb.append("Thumbnail SHA-256: ").append(thumbSha).append("\n");
                    putText(zip, "integrity.txt", sb.toString());
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
                zip.finish();
            }
        };

        return ResponseEntity.ok().headers(headers).body(body);
    }

    /* --------------------------------
     * Owner revokes a capability token
     * -------------------------------- */
    @PostMapping("/share/{token}/revoke")
    public ShareLink revoke(@PathVariable String token){
        return shareService.revoke(token);
    }

    /* --------------------------------------------
     * Auth-only: claim anonymous evidence to owner
     * -------------------------------------------- */
    @PostMapping("/evidence/{id}/claim")
    public Evidence claim(@PathVariable UUID id,
                          @RequestParam String token,
                          @AuthenticationPrincipal org.rights.locker.Entities.AppUser current) {
        if (current == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var share = shareService.requireActive(token);
        if (!share.getEvidenceId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token does not match evidence");
        }
        var ev = evidenceRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ev.setOwner(current);
        ev = evidenceRepo.save(ev);
        shareService.revoke(token);
        return ev;
    }

    /* --------------- helpers --------------- */

    private String presign(String bucket, String key, Duration ttl) {
        if (key == null || key.isBlank()) return null;
        var get = software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                .bucket(bucket).key(key).build();
        var pre = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(get)
                .build();
        return presigner.presignGetObject(pre).url().toString();
    }

    private static long copyToZip(ZipOutputStream zip, ResponseInputStream<?> in, String name, MessageDigest md)
            throws java.io.IOException {
        zip.putNextEntry(new ZipEntry(name));
        byte[] buf = new byte[8192];
        int r; long size = 0;
        while ((r = in.read(buf)) != -1) {
            md.update(buf, 0, r);
            zip.write(buf, 0, r);
            size += r;
        }
        zip.closeEntry();
        in.close();
        return size;
    }

    private static void putText(ZipOutputStream zip, String name, String text) throws java.io.IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(text.getBytes());
        zip.closeEntry();
    }

    private static String guessExt(String key) {
        String k = key.toLowerCase();
        if (k.endsWith(".mp4")) return ".mp4";
        if (k.endsWith(".mov")) return ".mov";
        if (k.endsWith(".mkv")) return ".mkv";
        if (k.endsWith(".jpg") || k.endsWith(".jpeg")) return ".jpg";
        if (k.endsWith(".png")) return ".png";
        return "";
    }
}
