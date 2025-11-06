package org.rights.locker.Services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PDFBuilderService {

    private final ObjectMapper om;

    /** Build a simple, monospaced PDF containing pretty-printed metadata JSON. */
    public byte[] buildMetadataPdf(Map<String, Object> metadata) {
        try {
            String json = om.writerWithDefaultPrettyPrinter().writeValueAsString(metadata);

            try (PDDocument doc = new PDDocument()) {
                PDPage page = new PDPage(PDRectangle.LETTER);
                doc.addPage(page);

                final PDFont bodyFont = PDType1Font.COURIER;
                final PDFont titleFont = PDType1Font.HELVETICA_BOLD;
                final float bodySize = 9f;
                final float leading = bodySize * 1.2f;

                final float margin = 40f;
                float startX = margin;
                float contentWidth = page.getMediaBox().getWidth() - 2 * margin;
                float y = page.getMediaBox().getHeight() - margin;

                // Title
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(titleFont, 14);
                    cs.newLineAtOffset(startX, y);
                    cs.showText("RightsLocker Metadata");
                    cs.endText();
                }
                y -= 24f;

                List<String> lines = wrapText(json, bodyFont, bodySize, contentWidth);

                // Write body; open/close a content stream per page
                PDPageContentStream cs = new PDPageContentStream(doc, page);
                cs.setFont(bodyFont, bodySize);

                for (String line : lines) {
                    if (y < margin) {
                        cs.close();
                        page = new PDPage(PDRectangle.LETTER);
                        doc.addPage(page);
                        y = page.getMediaBox().getHeight() - margin;
                        cs = new PDPageContentStream(doc, page);
                        cs.setFont(bodyFont, bodySize);
                    }
                    cs.beginText();
                    cs.newLineAtOffset(startX, y);
                    cs.showText(line);
                    cs.endText();
                    y -= leading;
                }

                cs.close();

                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    doc.save(baos);
                    return baos.toByteArray();
                }
            }
        } catch (Exception e) {
            // Fail-soft so ZIP generation still succeeds
            return minimalErrorPdf("PDF generation error: " + e.getMessage());
        }
    }

    /** Word-wrap preserving whitespace tokens to fit the page width. */
    private static List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> out = new ArrayList<>();
        for (String rawLine : text.split("\\r?\\n")) {
            StringBuilder line = new StringBuilder();
            for (String tok : rawLine.split("(?<=\\s)|(?=\\s)")) { // keep spaces as tokens
                String candidate = line + tok;
                float w = font.getStringWidth(candidate) / 1000f * fontSize;
                if (w > maxWidth && line.length() > 0) {
                    out.add(line.toString());
                    line = new StringBuilder(tok);
                } else {
                    line.append(tok);
                }
            }
            out.add(line.toString());
        }
        return out;
    }

    /** Very small one-page PDF fallback; if PDFBox itself fails, return UTF-8 bytes. */
    private static byte[] minimalErrorPdf(String msg) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(40, page.getMediaBox().getHeight() - 40);
                cs.showText(msg);
                cs.endText();
            }
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                doc.save(baos);
                return baos.toByteArray();
            }
        } catch (IOException ioe) {
            return msg.getBytes(StandardCharsets.UTF_8);
        }
    }
}
