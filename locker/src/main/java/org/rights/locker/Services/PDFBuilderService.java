package org.rights.locker.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PDFBuilderService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;
    private static final DateTimeFormatter READABLE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
    private static final float MARGIN = 54f;
    private static final float LINE_HEIGHT = 14f;
    private static final float SECTION_SPACING = 20f;

    public byte[] buildMetadataPdf(Map<String, Object> m) {
        try (PDDocument doc = new PDDocument()) {
            PdfContext ctx = new PdfContext(doc, MARGIN);

            // Header
            ctx.addText("RightsLocker Evidence Metadata", PDType1Font.HELVETICA_BOLD, 16);
            ctx.addText("Generated: " + ISO.format(Instant.now()), PDType1Font.HELVETICA, 11);
            ctx.addSpacing(SECTION_SPACING);

            // Identity
            ctx.addSection("Identity");
            ctx.addLine("Evidence ID", s(m,"evidenceId"));
            ctx.addLine("Title", s(m,"title"));
            ctx.addWrappedLine("Description", s(m,"description"));
            ctx.addLine("Captured At", formatDateTime(s(m,"capturedAt"), s(m,"tzMinutes")));
            ctx.addLine("Ingested At", formatDateTime(s(m,"ingestedAt")));
            ctx.addSpacing(SECTION_SPACING);

            // Integrity
            ctx.addSection("Original File Integrity");
            ctx.addLine("Hash Algorithm", coalesce(s(m,"hashAlgorithm"), "SHA-256"));
            ctx.addWrappedLine("SHA-256", s(m,"originalSha256", "originalSha256", "original.sha256", "originalSha"));
            ctx.addLine("Size (bytes)", formatBytes(s(m,"originalSizeB", "original.sizeBytes")));
            ctx.addSpacing(SECTION_SPACING);

            // Capture/Device
            ctx.addSection("Capture & Device");
            ctx.addLine("Original Capture Time", formatDateTime(s(m,"dateOriginal")));
            ctx.addLine("Time Zone Offset (minutes)", s(m,"tzMinutes"));
            ctx.addLine("GPS (lat,lon)", join(s(m,"lat"), s(m,"lon")));
            ctx.addLine("Altitude (m)", s(m,"altitudeM"));
            ctx.addLine("Heading (deg)", s(m,"headingDeg"));
            ctx.addLine("Camera Make", s(m,"cameraMake"));
            ctx.addLine("Camera Model", s(m,"cameraModel"));
            ctx.addLine("Lens Model", s(m,"lensModel"));
            ctx.addWrappedLine("Software", s(m,"software"));
            ctx.addLine("Image WxH (px)", join(s(m,"widthPx"), s(m,"heightPx"), "×"));
            ctx.addLine("Orientation (deg)", s(m,"orientationDeg"));
            ctx.addSpacing(SECTION_SPACING);

            // Video Properties
            ctx.addSection("Video Properties");
            ctx.addLine("Container", s(m,"container"));
            ctx.addLine("Video Codec", s(m,"videoCodec"));
            ctx.addLine("Audio Codec", s(m,"audioCodec"));
            ctx.addLine("Duration", formatDuration(s(m,"durationMs")));
            ctx.addLine("Frame Rate (fps)", s(m,"videoFps"));
            ctx.addLine("Rotation (deg)", s(m,"videoRotationDeg"));
            ctx.addSpacing(SECTION_SPACING);

            // Authenticity Assessment
            ctx.addSection("Authenticity Assessment");
            ctx.addLine("Provenance Status", s(m,"provenanceStatus"));
            ctx.addLine("Metadata Integrity", s(m,"metadataIntegrity"));
            ctx.addLine("Synthetic Media Risk", s(m,"syntheticMediaRisk"));
            ctx.addWrappedLine("Manipulation Signals", s(m,"manipulationSignals"));
            ctx.addWrappedLine("Assessment Summary", s(m,"assessmentSummary"));

            // Access/Share Context
            if (m.containsKey("shareToken")) {
                ctx.addSpacing(SECTION_SPACING);
                ctx.addSection("Access Context");
                ctx.addWrappedLine("Share Token", s(m,"shareToken"));
                ctx.addLine("Share Allows Original", s(m,"shareAllowOriginal"));
                ctx.addLine("Share Expires At", formatDateTime(s(m,"shareExpiresAt")));
            }

            ctx.finish();

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                doc.save(bos);
                return bos.toByteArray();
            }
        } catch (Exception e) {
            log.warn("PDF build failed: {}", e.getMessage(), e);
            return new byte[0];
        }
    }

    private static class PdfContext {
        private final PDDocument doc;
        private final float margin;
        private final float pageWidth;
        private final float pageHeight;
        private final float maxWidth;

        private PDPage currentPage;
        private PDPageContentStream currentStream;
        private float currentY;
        private boolean textStarted = false;

        public PdfContext(PDDocument doc, float margin) throws Exception {
            this.doc = doc;
            this.margin = margin;

            PDRectangle rect = PDRectangle.LETTER;
            this.pageWidth = rect.getWidth();
            this.pageHeight = rect.getHeight();
            this.maxWidth = pageWidth - (2 * margin);

            startNewPage();
        }

        private void startNewPage() throws Exception {
            if (currentStream != null) {
                if (textStarted) {
                    currentStream.endText();
                    textStarted = false;
                }
                currentStream.close();
            }

            currentPage = new PDPage(PDRectangle.LETTER);
            doc.addPage(currentPage);
            currentStream = new PDPageContentStream(doc, currentPage);
            currentY = pageHeight - margin;
        }

        private void ensureSpace(float needed) throws Exception {
            if (currentY - needed < margin) {
                startNewPage();
            }
        }

        private void startText() throws Exception {
            if (!textStarted) {
                currentStream.beginText();
                currentStream.newLineAtOffset(margin, currentY);
                textStarted = true;
            }
        }

        public void addText(String text, PDFont font, float size) throws Exception {
            if (text == null || text.trim().isEmpty()) return;

            ensureSpace(size + 5);
            startText();

            currentStream.setFont(font, size);
            currentStream.showText(text);
            currentStream.newLine();
            currentY -= LINE_HEIGHT;
        }

        public void addSection(String title) throws Exception {
            addSpacing(10);
            addText(title, PDType1Font.HELVETICA_BOLD, 12);
        }

        public void addLine(String key, String value) throws Exception {
            String displayValue = (value == null || value.trim().isEmpty()) ? "—" : value;
            addText(key + ": " + displayValue, PDType1Font.HELVETICA, 11);
        }

        public void addWrappedLine(String key, String value) throws Exception {
            if (value == null || value.trim().isEmpty()) {
                addLine(key, "—");
                return;
            }

            try {
                List<String> lines = wrapText(key + ": " + value, PDType1Font.HELVETICA, 11, maxWidth);
                for (String line : lines) {
                    addText(line, PDType1Font.HELVETICA, 11);
                }
            } catch (Exception e) {
                // Fallback to simple line if wrapping fails
                addLine(key, value);
            }
        }

        private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws Exception {
            List<String> lines = new ArrayList<>();
            String[] words = text.split("\\s+");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                float textWidth = font.getStringWidth(testLine) / 1000 * fontSize;

                if (textWidth <= maxWidth) {
                    currentLine = new StringBuilder(testLine);
                } else {
                    if (currentLine.length() > 0) {
                        lines.add(currentLine.toString());
                        currentLine = new StringBuilder(word);
                    } else {
                        // Single word is too long, break it
                        lines.add(word);
                    }
                }
            }

            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }

            return lines;
        }

        public void addSpacing(float amount) throws Exception {
            ensureSpace(amount);
            currentY -= amount;
        }

        public void finish() throws Exception {
            if (currentStream != null) {
                if (textStarted) {
                    currentStream.endText();
                }
                currentStream.close();
            }
        }
    }

    /* --- Helper Methods --- */

    private static String s(Map<String,Object> m, String... keys){
        for (String k: keys){
            Object v = m.get(k);
            if (v != null) {
                String str = v.toString().trim();
                return str.isEmpty() ? null : str;
            }
        }
        return null;
    }

    private static String formatDateTime(String dateStr, String tzOffsetStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;

        try {
            Instant instant;

            // Try to parse as ISO instant first
            try {
                instant = Instant.parse(dateStr);
            } catch (DateTimeParseException e) {
                // Try parsing as epoch seconds or milliseconds
                try {
                    long value = Long.parseLong(dateStr);
                    instant = value > 1_000_000_000_000L ? 
                        Instant.ofEpochMilli(value) : 
                        Instant.ofEpochSecond(value);
                } catch (NumberFormatException nfe) {
                    return dateStr; // Return as-is if can't parse
                }
            }

            // Apply timezone offset if available
            ZoneOffset offset = ZoneOffset.UTC;
            if (tzOffsetStr != null && !tzOffsetStr.trim().isEmpty()) {
                try {
                    int offsetMinutes = Integer.parseInt(tzOffsetStr);
                    offset = ZoneOffset.ofTotalSeconds(offsetMinutes * 60);
                } catch (NumberFormatException ignored) {
                    // Use UTC as fallback
                }
            }

            LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, offset);
            return localDateTime.format(READABLE.withZone(offset));

        } catch (Exception e) {
            return dateStr; // Return original if any parsing fails
        }
    }

    private static String formatDateTime(String dateStr) {
        return formatDateTime(dateStr, null);
    }

    private static String formatBytes(String bytesStr) {
        if (bytesStr == null || bytesStr.trim().isEmpty()) return null;

        try {
            long bytes = Long.parseLong(bytesStr);
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        } catch (NumberFormatException e) {
            return bytesStr;
        }
    }

    private static String formatDuration(String durationMs) {
        if (durationMs == null || durationMs.trim().isEmpty()) return null;

        try {
            long ms = Long.parseLong(durationMs);
            long seconds = ms / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;

            if (hours > 0) {
                return String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60);
            } else if (minutes > 0) {
                return String.format("%d:%02d", minutes, seconds % 60);
            } else {
                return seconds + "s";
            }
        } catch (NumberFormatException e) {
            return durationMs + " ms";
        }
    }

    private static String join(String a, String b){ 
        return (a==null&&b==null) ? null : (n(a) + ", " + n(b)); 
    }

    private static String join(String a, String b, String sep){ 
        return (a==null&&b==null) ? null : (n(a) + sep + n(b)); 
    }

    private static String n(String s){ 
        return s==null ? "—" : s; 
    }

    private static String coalesce(String v, String d){ 
        return (v==null||v.trim().isEmpty()) ? d : v; 
    }
}
