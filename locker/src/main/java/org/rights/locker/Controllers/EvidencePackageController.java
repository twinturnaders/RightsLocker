package org.rights.locker.Controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rights.locker.Config.AppProps;
import org.rights.locker.Entities.AppUser;
import org.rights.locker.Entities.Evidence;
import org.rights.locker.Repos.EvidenceRepo;
import org.rights.locker.Security.UserPrincipal;
import org.rights.locker.Services.PDFBuilderService;
import org.rights.locker.Services.UserPrincipalService;
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
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@RestController
@RequestMapping("/api/evidence")
@RequiredArgsConstructor
public class EvidencePackageController {

    private final EvidenceRepo evidenceRepo;
    private final S3Client s3;
    private final ObjectMapper om;
    private final AppProps app;
    private final PDFBuilderService pdfService;
    private final UserPrincipalService principalService;

    @Value("${app.s3.bucketOriginals}") private String bucketOriginals;
    @Value("${app.s3.bucketHot}") private String bucketHot;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;
    private static final int BUF = 8192;

    @GetMapping("/{id}/package")
    public ResponseEntity<StreamingResponseBody> downloadPackage(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "original") String type,
            @RequestParam(defaultValue = "true") boolean includeThumb,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = principalService.requireUser(principal);

        Evidence ev = evidenceRepo.findByIdAndOwner(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        boolean hasRedacted = ev.getRedactedKey() != null && !ev.getRedactedKey().isBlank();
        String key = "original".equalsIgnoreCase(type) ? ev.getOriginalKey()
                : (hasRedacted ? ev.getRedactedKey() : ev.getOriginalKey());
        String bucket = "original".equalsIgnoreCase(type) ? bucketOriginals
                : (hasRedacted ? bucketHot : bucketOriginals);
        String filenameBase = ("original".equalsIgnoreCase(type) || !hasRedacted) ? "original" : "redacted";

        if (key == null || key.isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Primary media is not available yet.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("evidence-" + safeFile(ev.getId() + ".zip")).build());

        StreamingResponseBody body = out -> {
            try (ZipOutputStream zip = new ZipOutputStream(out)) {
                // 1) media
                String mediaShaHex;
                long mediaSize;
                try (ResponseInputStream<GetObjectResponse> mediaIn =
                             s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())) {
                    MessageDigest mediaMd = MessageDigest.getInstance("SHA-256");
                    mediaSize = copyToZip(zip, mediaIn, filenameBase + guessExt(key), mediaMd);
                    mediaShaHex = HexFormat.of().formatHex(mediaMd.digest());
                } catch (NoSuchKeyException nsk) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Media not available yet. Try later.");
                }

                // 2) optional thumbnail
                String thumbShaHex = null;
                if (includeThumb && ev.getThumbnailKey() != null && !ev.getThumbnailKey().isBlank()) {
                    try (ResponseInputStream<GetObjectResponse> th =
                                 s3.getObject(GetObjectRequest.builder().bucket(bucketHot).key(ev.getThumbnailKey()).build())) {
                        MessageDigest thMd = MessageDigest.getInstance("SHA-256");
                        copyToZip(zip, th, "thumb.jpg", thMd);
                        thumbShaHex = HexFormat.of().formatHex(thMd.digest());
                    } catch (Exception ignore) {}

                }

                // 3) metadata.json (include rich metadata)
                boolean isRedacted = hasRedacted && !("original".equalsIgnoreCase(type));
                Map<String,Object> metadata = new LinkedHashMap<>();
                metadata.put("evidenceId", ev.getId().toString());
                metadata.put("title", ev.getTitle());
                metadata.put("description", ev.getDescription());
                metadata.put("capturedAt", ev.getCapturedAt() != null ? ISO.format(ev.getCapturedAt()) : null);
                metadata.put("ingestedAt", ev.getCreatedAt() != null ? ISO.format(ev.getCreatedAt()) : null);
                metadata.put("processedAt", ISO.format(Instant.now()));
                metadata.put("hashAlgorithm", "SHA-256");
                metadata.put("original", Map.of(
                        "sha256", ev.getOriginalSha256(),
                        "sizeBytes", ev.getOriginalSizeB()
                ));
                metadata.put(isRedacted ? "redacted" : "delivered", Map.of(
                        "sha256", mediaShaHex,
                        "sizeBytes", mediaSize
                ));

                // enriched
                metadata.put("dateOriginal", ev.getExifDateOriginal());
                metadata.put("tzMinutes", ev.getTzOffsetMinutes());
                metadata.put("altitudeM", ev.getCaptureAltitudeM());
                metadata.put("headingDeg", ev.getCaptureHeadingDeg());
                metadata.put("cameraMake", ev.getCameraMake());
                metadata.put("cameraModel", ev.getCameraModel());
                metadata.put("lensModel", ev.getLensModel());
                metadata.put("software", ev.getSoftware());
                metadata.put("widthPx", ev.getWidthPx());
                metadata.put("heightPx", ev.getHeightPx());
                metadata.put("orientationDeg", ev.getOrientationDeg());
                metadata.put("container", ev.getContainer());
                metadata.put("videoCodec", ev.getVideoCodec());
                metadata.put("audioCodec", ev.getAudioCodec());
                metadata.put("durationMs", ev.getDurationMs());
                metadata.put("videoFps", ev.getVideoFps());
                metadata.put("videoRotationDeg", ev.getVideoRotationDeg());

                putText(zip, "metadata.json", om.writerWithDefaultPrettyPrinter().writeValueAsString(metadata));

                // 4) metadata.pdf (beautified)
                byte[] metaPdf = pdfService.buildMetadataPdf(metadata);
                putBytes(zip, "metadata.pdf", metaPdf);

                // 5) integrity.txt
                StringBuilder sb = new StringBuilder();
                sb.append("RightsLocker Integrity Summary\n");
                sb.append("Issuer: ").append(app.getAttestation().getIssuer()).append("\n");
                sb.append("Evidence ID: ").append(ev.getId()).append("\n\n");
                sb.append("Original SHA-256: ").append(nullToDash(ev.getOriginalSha256())).append("\n");
                sb.append("Delivered (").append(filenameBase).append(") SHA-256: ").append(mediaShaHex).append("\n");
                if (thumbShaHex != null) sb.append("Thumbnail SHA-256: ").append(thumbShaHex).append("\n");
                putText(zip, "integrity.txt", sb.toString());

                zip.finish();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Failed to generate package", e);
            }
        };

        return ResponseEntity.ok().headers(headers).body(body);
    }

    /* helpers (same as before) */
    static long copyToZip(ZipOutputStream zip, InputStream in, String name, MessageDigest md) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        byte[] buf = new byte[BUF];
        int r; long size = 0;
        while ((r = in.read(buf)) != -1) {
            md.update(buf, 0, r);
            zip.write(buf, 0, r);
            size += r;
        }
        zip.closeEntry();
        return size;
    }
    static void putText(ZipOutputStream zip, String name, String text) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(text.getBytes());
        zip.closeEntry();
    }
    static void putBytes(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }
    static String guessExt(String key) {
        String k = key == null ? "" : key.toLowerCase();
        if (k.endsWith(".mp4")) return ".mp4";
        if (k.endsWith(".mov")) return ".mov";
        if (k.endsWith(".mkv")) return ".mkv";
        if (k.endsWith(".jpg") || k.endsWith(".jpeg")) return ".jpg";
        if (k.endsWith(".png")) return ".png";
        return "";
    }
    private static String nullToDash(String s){ return (s == null || s.isBlank()) ? "-" : s; }
    private static String safeFile(String name) {
        return name.replaceAll("[\\r\\n\\t\\\\/:*?\"<>|]", "_");
    }
}
