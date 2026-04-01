package org.rights.locker.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PDFBuilderService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    public byte[] buildMetadataPdf(Map<String, Object> m) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float x = 54, y = page.getMediaBox().getUpperRightY() - 54;
                cs.setLeading(16);

                // header
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
                cs.newLineAtOffset(x, y);
                cs.showText("RightsLocker Evidence Metadata");
                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.newLine();
                cs.showText("Generated: " + ISO.format(Instant.now()));
                cs.newLine();
                cs.newLine();

                // identity
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.showText("Identity");
                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.newLine();
                line(cs, "Evidence ID", s(m,"evidenceId"));
                line(cs, "Title", s(m,"title"));
                line(cs, "Description", s(m,"description"));
                line(cs, "Captured At (UTC)", s(m,"capturedAt"));
                line(cs, "Ingested At (UTC)", s(m,"ingestedAt"));
                cs.newLine();

                // integrity
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.showText("Original File Integrity");
                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.newLine();
                line(cs, "Hash Algorithm", coalesce(s(m,"hashAlgorithm"), "SHA-256"));
                line(cs, "SHA-256", s(m,"originalSha256", "originalSha256", "original.sha256", "originalSha"));
                line(cs, "Size (bytes)", s(m,"originalSizeB", "original.sizeBytes"));
                cs.newLine();

                // capture/device (from extraction)
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.showText("Capture & Device");
                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.newLine();
                line(cs, "Original Capture Time", s(m,"dateOriginal"));
                line(cs, "Time Zone Offset (minutes)", s(m,"tzMinutes"));
                line(cs, "GPS (lat,lon)", join(s(m,"lat"), s(m,"lon")));
                line(cs, "Altitude (m)", s(m,"altitudeM"));
                line(cs, "Heading (deg)", s(m,"headingDeg"));
                line(cs, "Camera Make", s(m,"cameraMake"));
                line(cs, "Camera Model", s(m,"cameraModel"));
                line(cs, "Lens Model", s(m,"lensModel"));
                line(cs, "Software", s(m,"software"));
                line(cs, "Image WxH (px)", join(s(m,"widthPx"), s(m,"heightPx"), "×"));
                line(cs, "Orientation (deg)", s(m,"orientationDeg"));
                cs.newLine();

                // video props
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.showText("Video Properties");
                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.newLine();
                line(cs, "Container", s(m,"container"));
                line(cs, "Video Codec", s(m,"videoCodec"));
                line(cs, "Audio Codec", s(m,"audioCodec"));
                line(cs, "Duration (ms)", s(m,"durationMs"));
                line(cs, "Frame Rate (fps)", s(m,"videoFps"));
                line(cs, "Rotation (deg)", s(m,"videoRotationDeg"));
                cs.newLine();

                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.showText("Authenticity Assessment");
                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.newLine();
                line(cs, "Provenance Status", s(m,"provenanceStatus"));
                line(cs, "Metadata Integrity", s(m,"metadataIntegrity"));
                line(cs, "Synthetic Media Risk", s(m,"syntheticMediaRisk"));
                line(cs, "Manipulation Signals", s(m,"manipulationSignals"));
                line(cs, "Assessment Summary", s(m,"assessmentSummary"));
                cs.newLine();

                // access/share context if present
                if (m.containsKey("shareToken")) {
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    cs.showText("Access Context");
                    cs.setFont(PDType1Font.HELVETICA, 11);
                    cs.newLine();
                    line(cs, "Share Token", s(m,"shareToken"));
                    line(cs, "Share Allows Original", s(m,"shareAllowOriginal"));
                    line(cs, "Share Expires At", s(m,"shareExpiresAt"));
                }

                cs.endText();
            }

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                doc.save(bos);
                return bos.toByteArray();
            }
        } catch (Exception e) {
            log.warn("PDF build failed: {}", e.getMessage(), e);
            return new byte[0];
        }
    }

    /* --- helpers --- */
    private static void line(PDPageContentStream cs, String k, String v) throws Exception {
        cs.showText(k + ": " + (v == null || v.isBlank() ? "—" : v));
        cs.newLine();
    }
    private static String s(Map<String,Object> m, String... keys){
        for (String k: keys){
            Object v = m.get(k);
            if (v != null) return v.toString();
        }
        return null;
    }
    private static String join(String a, String b){ return (a==null&&b==null)?"":(n(a)+" , "+n(b)); }
    private static String join(String a, String b, String sep){ return (a==null&&b==null)?"":(n(a)+sep+n(b)); }
    private static String n(String s){ return s==null?"—":s; }
    private static String coalesce(String v, String d){ return (v==null||v.isBlank())?d:v; }
}
