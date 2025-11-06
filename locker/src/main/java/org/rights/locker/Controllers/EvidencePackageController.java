package org.rights.locker.Controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rights.locker.Config.AppProps;
import org.rights.locker.Entities.AppUser;
import org.rights.locker.Entities.Evidence;
import org.rights.locker.Repos.EvidenceRepo;
import org.rights.locker.Services.PDFBuilderService;
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
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
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

    @Value("${app.s3.bucketOriginals}") private String bucketOriginals;
    @Value("${app.s3.bucketHot}") private String bucketHot;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;
    private static final int BUF = 8192;

    @GetMapping("/{id}/package")
    public ResponseEntity<StreamingResponseBody> downloadPackage(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "original") String type,
            @RequestParam(defaultValue = "true") boolean includeThumb,
            @AuthenticationPrincipal AppUser current
    ) {
        if (current == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        Evidence ev = evidenceRepo.findByIdAndOwner(id, current)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        boolean hasRedacted = ev.getRedactedKey() != null && !ev.getRedactedKey().isBlank();
        String key = "original".equalsIgnoreCase(type) ? ev.getOriginalKey()
                : (hasRedacted ? ev.getRedactedKey() : ev.getOriginalKey());
        String bucket = "original".equalsIgnoreCase(type) ? bucketOriginals
                : (hasRedacted ? bucketHot : bucketOriginals);
        String filenameBase = ("original".equalsIgnoreCase(type) || !hasRedacted) ? "original" : "redacted";

        if (key == null || key.isBlank())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Primary media is not available yet.");

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

                // 3) metadata.json
                boolean isRedacted = hasRedacted && !("original".equalsIgnoreCase(type));
                Map<String,Object> metadata = Map.of(
                        "evidenceId", ev.getId().toString(),
                        "title", ev.getTitle(),
                        "capturedAt", ev.getCapturedAt() != null ? ISO.format(ev.getCapturedAt()) : null,
                        "ingestedAt", ev.getCreatedAt() != null ? ISO.format(ev.getCreatedAt()) : null,
                        "processedAt", ISO.format(Instant.now()),
                        "original", Map.of(
                                "sha256", ev.getOriginalSha256(),
                                "sizeBytes", ev.getOriginalSizeB()
                        ),
                        (isRedacted ? "redacted" : "delivered"), Map.of(
                                "sha256", mediaShaHex,
                                "sizeBytes", mediaSize
                        )
                );
                putText(zip, "metadata.json", om.writerWithDefaultPrettyPrinter().writeValueAsString(metadata));

                // 4) metadata.pdf
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

                // 6) printable HTML report (optional)
                putText(zip, "Evidence_Report.html",
                        buildHtmlReport(ev, ev.getOriginalSha256(),
                                ev.getOriginalSizeB() == null ? -1 : ev.getOriginalSizeB(),
                                isRedacted, mediaShaHex, mediaSize));

                // 7) optional HMAC signature over metadata.json
                if (app.getAttestation().isEnabled()
                        && app.getAttestation().getHmacSecret() != null
                        && !app.getAttestation().getHmacSecret().isBlank()) {
                    byte[] metaBytes = om.writeValueAsBytes(metadata);
                    var mac = javax.crypto.Mac.getInstance("HmacSHA256");
                    mac.init(new javax.crypto.spec.SecretKeySpec(
                            app.getAttestation().getHmacSecret().getBytes(), "HmacSHA256"));
                    String sig = HexFormat.of().formatHex(mac.doFinal(metaBytes));
                    putText(zip, "attestation.sig",
                            sig + "\nalg=HMAC-SHA256\nissuer=" + app.getAttestation().getIssuer() + "\n");
                }

                zip.finish();
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                throw new RuntimeException("Failed to generate package", e);
            }
        };

        return ResponseEntity.ok().headers(headers).body(body);
    }

    /* helpers */
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

    private String buildHtmlReport(Evidence ev, String originalSha, long originalSize,
                                   boolean isRedacted, String deliveredSha, long deliveredSize) {
        String now = ISO.format(Instant.now());
        String cap = ev.getCapturedAt() != null ? ISO.format(ev.getCapturedAt()) : "—";
        String title = ev.getTitle() != null ? ev.getTitle() : "Untitled";
        String deliveredKind = isRedacted ? "Redacted" : "Original";

        return """
<!doctype html>
<html lang="en">
<head><meta charset="utf-8"><title>RightsLocker Evidence Report</title>
<style>
  body{font-family:system-ui,Segoe UI,Roboto,Helvetica,Arial,sans-serif;color:#111;margin:24px;line-height:1.45}
  .card{border:1px solid #e5e7eb;border-radius:12px;padding:16px;margin:12px 0}
  h1{margin:0 0 8px} h2{margin:16px 0 8px}
  table{width:100%;border-collapse:collapse;margin-top:8px}
  td,th{border:1px solid #e5e7eb;padding:8px;text-align:left;font-size:14px}
  .mono{font-family:ui-monospace,SFMono-Regular,Consolas,monospace;font-size:13px}
</style></head>
<body>
  <h1>RightsLocker Evidence Report</h1>
  <div>Issued: %s · Issuer: %s</div>

  <div class="card">
    <table>
      <tr><th>Evidence ID</th><td>%s</td></tr>
      <tr><th>Title</th><td>%s</td></tr>
      <tr><th>Captured at (UTC)</th><td>%s</td></tr>
    </table>
  </div>

  <h2>Integrity</h2>
  <table>
    <tr><th>Item</th><th>SHA-256</th><th>Size (bytes)</th></tr>
    <tr><td>Original</td><td class="mono">%s</td><td>%s</td></tr>
    <tr><td>%s (delivered)</td><td class="mono">%s</td><td>%s</td></tr>
  </table>

  <h2>Processing</h2>
  <div class="card">
    <div><b>Method:</b> %s</div>
    <div style="color:#6b7280">If enabled, faces detected and blurred automatically.</div>
  </div>

  <div style="color:#6b7280;margin-top:24px">
    Note: Times are UTC. This report summarizes digests and processing steps applied by RightsLocker.
  </div>
</body>
</html>
""".formatted(
                now, app.getAttestation().getIssuer(),
                ev.getId(), title, cap,
                nullToDash(originalSha), (originalSize >= 0 ? Long.toString(originalSize) : "—"),
                deliveredKind, deliveredSha, (deliveredSize >= 0 ? Long.toString(deliveredSize) : "—"),
                (isRedacted ? "Face blur (automatic)" : "None")
        );
    }
}
