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
    private final ShareLinkRepo shareLinkRepo;

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
    @GetMapping("/api/share/{token}/package")
    public ResponseEntity<StreamingResponseBody> downloadSharedPackage(
            @PathVariable String token,
            @RequestParam(defaultValue = "redacted") String type
    ) {
        var link = shareLinkRepo.findByToken(token).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var ev = evidenceRepo.findById(link.getEvidenceId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Decide which object we’ll include
        String chosenKey = switch (type.toLowerCase()) {
            case "redacted" -> (ev.getRedactedKey() != null ? ev.getRedactedKey() : ev.getOriginalKey());
            case "original" -> ev.getOriginalKey();
            default -> ev.getRedactedKey();
        };
        if (chosenKey == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No asset available to package");

        StreamingResponseBody body = out -> {
            try (var zos = new java.util.zip.ZipOutputStream(out)) {

                // 1) manifest.json (no nulls)
                var manifest = mapNonNull(
                        "evidenceId", ev.getId().toString(),
                        "title", ev.getTitle(),
                        "description", ev.getDescription(),
                        "capturedAt", ev.getCapturedAt(),
                        "status", ev.getStatus() != null ? ev.getStatus().name() : null,
                        "includedObjectKey", chosenKey
                );
                var manifestBytes = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(manifest);
                zos.putNextEntry(new java.util.zip.ZipEntry("manifest.json"));
                zos.write(manifestBytes);
                zos.closeEntry();

                // 2) media (stream from S3)
                zos.putNextEntry(new java.util.zip.ZipEntry(filenameForKey(chosenKey)));
                s3.getObject(b -> b.bucket(bucketHot).key(chosenKey)).transferTo(zos);
                zos.closeEntry();

                // 3) metadata.pdf (try to generate; skip on failure)
                byte[] pdfBytes = null;
                try {
                    pdfBytes = generateMetadataPdf(ev, link);
                } catch (Exception ignore) { /* skip pdf gracefully */ }

                if (pdfBytes != null && pdfBytes.length > 0) {
                    zos.putNextEntry(new java.util.zip.ZipEntry("metadata.pdf"));
                    zos.write(pdfBytes);
                    zos.closeEntry();
                }

                zos.finish();
            }
        };

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"evidence-" + ev.getId() + ".zip\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }

    /* owner revokes share */
    @PostMapping("/share/{token}/revoke")
    public ShareLink revoke(@PathVariable String token){
        return shareService.revoke(token);
    }

    /* helpers */
    private byte[] generateMetadataPdf(Evidence ev, ShareLink link) {
        try (var doc = new org.apache.pdfbox.pdmodel.PDDocument()) {
            var page = new org.apache.pdfbox.pdmodel.PDPage();
            doc.addPage(page);

            var font = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA;
            try (var content = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
                content.beginText();
                content.setFont(font, 12);
                content.newLineAtOffset(50, 750);
                content.showText("Evidence Metadata");
                content.newLineAtOffset(0, -20);
                content.showText("ID: " + ev.getId());
                content.newLineAtOffset(0, -20);
                content.showText("Title: " + safe(ev.getTitle()));
                content.newLineAtOffset(0, -20);
                content.showText("Captured At: " + String.valueOf(ev.getCapturedAt()));
                content.newLineAtOffset(0, -20);
                content.showText("Share Token: " + link.getToken());
                content.endText();
            }

            try (var bos = new java.io.ByteArrayOutputStream()) {
                doc.save(bos);
                return bos.toByteArray();
            }
        } catch (Exception e) {
            log.warn("PDF generation failed for evidence {}", ev.getId(), e);
            return new byte[0];
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static String filenameForKey(String key) {
        // If your keys are "redacted/<id>.mp4", keep the leaf name
        int i = key.lastIndexOf('/');
        return i >= 0 ? key.substring(i + 1) : key;
    }
    static Map<String,Object> mapNonNull(Object... kv) {
        Map<String,Object> m = new java.util.LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            String k = (String) kv[i];
            Object v = kv[i + 1];
            if (k != null && v != null) m.put(k, v);
        }
        return java.util.Collections.unmodifiableMap(m);
    }
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
