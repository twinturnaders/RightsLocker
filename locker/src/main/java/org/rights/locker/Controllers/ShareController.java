package org.rights.locker.Controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rights.locker.Entities.Evidence;
import org.rights.locker.Entities.ShareLink;
import org.rights.locker.Repos.EvidenceRepo;
import org.rights.locker.Services.PDFBuilderService;
import org.rights.locker.Services.ShareService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.rights.locker.Controllers.EvidencePackageController.*;

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

    /* owner/system creates a share link */
    @PostMapping("/evidence/{id}/share")
    public ShareLink create(@PathVariable UUID id, @RequestBody org.rights.locker.DTOs.ShareCreateRequest req){
        return shareService.create(id, req.expiresAt(), req.allowOriginal());
    }

    /* public: small metadata + short-lived presigned URLs */
    @GetMapping("/share/{token}")
    public ResponseEntity<?> getShare(@PathVariable String token){
        var share = shareService.requireActive(token);
        var ev = evidenceRepo.findById(share.getEvidenceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidence not found"));

        boolean hasRedacted = ev.getRedactedKey() != null && !ev.getRedactedKey().isBlank();
        boolean hasThumb = ev.getThumbnailKey() != null && !ev.getThumbnailKey().isBlank();

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

    /* public: printable metadata pdf */
    @GetMapping(value = "/share/{token}/metadata.pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> shareMetadataPdf(@PathVariable String token) {
        var s = shareService.requireActive(token);
        var ev = evidenceRepo.findById(s.getEvidenceId()).orElseThrow();

        var metadata = Map.of(
                "evidenceId", ev.getId().toString(),
                "title", ev.getTitle(),
                "capturedAt", ev.getCapturedAt() == null ? null : DateTimeFormatter.ISO_INSTANT.format(ev.getCapturedAt()),
                "ingestedAt", ev.getCreatedAt() == null ? null : DateTimeFormatter.ISO_INSTANT.format(ev.getCreatedAt()),
                "generatedAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                "access", Map.of("viaShareToken", s.getToken(), "allowOriginal", s.isAllowOriginal())
        );

        byte[] pdf = pdfService.buildMetadataPdf(metadata);
        return ResponseEntity.ok()
                .header("Content-Disposition","inline; filename=\"metadata.pdf\"")
                .body(pdf);
    }

    /* public: packaged zip (media + metadata.json + metadata.pdf + integrity.txt) with S3 preflight */
    @GetMapping("/share/{token}/package")
    public ResponseEntity<StreamingResponseBody> downloadSharedPackage(
            @PathVariable String token,
            @RequestParam(defaultValue = "redacted") String type) {

        var share = shareService.requireActive(token);
        var ev = evidenceRepo.findById(share.getEvidenceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidence not found"));

        final boolean wantOriginal = "original".equalsIgnoreCase(type);
        final boolean hasRedacted  = ev.getRedactedKey() != null && !ev.getRedactedKey().isBlank();
        boolean useOriginal = wantOriginal && share.isAllowOriginal();

        String key    = useOriginal ? ev.getOriginalKey() : (hasRedacted ? ev.getRedactedKey() : ev.getOriginalKey());

        if (key == null || key.isBlank()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not available");
        String bucket = useOriginal ? bucketOriginals : (hasRedacted ? bucketHot : bucketOriginals);
        String base   = (useOriginal || !hasRedacted) ? "original" : "redacted";

        String finalBucket = bucket;
        String finalKey = key;
        try {
            s3.headObject(b -> b.bucket(finalBucket).key(finalKey));
        } catch (S3Exception e) {
            if (!useOriginal && share.isAllowOriginal() && ev.getOriginalKey() != null) {
                bucket = bucketOriginals;
                key = ev.getOriginalKey();
                base = "original";
                s3.headObject(b -> b.bucket(finalBucket).key(finalKey)); // throw 409 if missing
            } else {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Media not ready. Try later.");
            }
        }
            final String fBucket = bucket, fKey = key, fBase = base;




        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("evidence-" + ev.getId() + ".zip").build());

        StreamingResponseBody body = out -> {
            try (var zip = new ZipOutputStream(out)) {
                // 1) media
                try (ResponseInputStream<GetObjectResponse> in = s3.getObject(b -> b.bucket(fBucket).key(fKey))) {
                    var md = MessageDigest.getInstance("SHA-256");
                    long size = copyToZip(zip, in, fBase + guessExt(fKey), md);
                    String sha = HexFormat.of().formatHex(md.digest());

                    // 2) optional thumb (best-effort)
                    String thumbSha = null;
                    if (ev.getThumbnailKey() != null && !ev.getThumbnailKey().isBlank()) {
                        try (ResponseInputStream<GetObjectResponse> th =
                                     s3.getObject(b -> b.bucket(bucketHot).key(ev.getThumbnailKey()))) {
                            var tmd = MessageDigest.getInstance("SHA-256");
                            copyToZip(zip, th, "thumb.jpg", tmd);
                            thumbSha = HexFormat.of().formatHex(tmd.digest());
                        } catch (Exception ignore) {}
                    }

                    // 3) metadata.json
                    var meta = Map.of(
                            "evidenceId", ev.getId().toString(),
                            "title", ev.getTitle(),
                            "capturedAt", ev.getCapturedAt(),
                            "deliveredKind", fBase,
                            "deliveredSha256", sha,
                            "deliveredSizeBytes", size
                    );
                    putText(zip, "metadata.json", om.writerWithDefaultPrettyPrinter().writeValueAsString(meta));

                    // 4) metadata.pdf
                    byte[] pdf = pdfService.buildMetadataPdf(Map.of(
                            "evidenceId", ev.getId().toString(),
                            "title", ev.getTitle(),
                            "capturedAt", ev.getCapturedAt(),
                            "generatedAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                            "access", Map.of("viaShareToken", share.getToken(), "allowOriginal", share.isAllowOriginal())
                    ));
                    putBytes(zip, "metadata.pdf", pdf);

                    // 5) integrity.txt
                    var sb = new StringBuilder();
                    sb.append("RightsLocker Public Package\n");
                    sb.append("Evidence ID: ").append(ev.getId()).append("\n");
                    sb.append("Delivered: ").append(fBase).append("\n");
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

    /* owner revokes share */
    @PostMapping("/share/{token}/revoke")
    public ShareLink revoke(@PathVariable String token){
        return shareService.revoke(token);
    }

    /* helpers */
    private String presign(String bucket, String key, Duration ttl) {
        if (key == null || key.isBlank()) return null;
        var req = GetObjectRequest.builder().bucket(bucket).key(key).build();
        return presigner.presignGetObject(b -> b.signatureDuration(ttl).getObjectRequest(req)).url().toString();
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

    private static void putBytes(ZipOutputStream zip, String name, byte[] bytes) throws java.io.IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }

    private static String guessExt(String key) {
        String k = key == null ? "" : key.toLowerCase();
        if (k.endsWith(".mp4")) return ".mp4";
        if (k.endsWith(".mov")) return ".mov";
        if (k.endsWith(".mkv")) return ".mkv";
        if (k.endsWith(".jpg") || k.endsWith(".jpeg")) return ".jpg";
        if (k.endsWith(".png")) return ".png";
        return "";
    }

}
