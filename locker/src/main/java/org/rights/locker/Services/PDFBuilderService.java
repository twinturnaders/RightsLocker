package org.rights.locker.Services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PDFBuilderService {

    private final ObjectMapper om;

    public byte[] buildMetadataPdf(Map<String, Object> metadata) throws IOException {
        String json = om.writerWithDefaultPrettyPrinter().writeValueAsString(metadata);

        try (var doc = new org.apache.pdfbox.pdmodel.PDDocument()) {
            var page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER);
            doc.addPage(page);

            var font = org.apache.pdfbox.pdmodel.font.PDType1Font.COURIER;
            float fontSize = 9f;
            float leading = 1.2f * fontSize;

            var mediaBox = page.getMediaBox();
            float margin = 40f;
            float width = mediaBox.getWidth() - 2*margin;
            float startX = margin;
            float startY = mediaBox.getHeight() - margin;

            try (var cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(font, 14);
                cs.newLineAtOffset(startX, startY);
                cs.showText("RightsLocker Metadata");
                cs.endText();

                float y = startY - 24f;

                var lines = wrapText(json, font, fontSize, width);
                cs.setFont(font, fontSize);
                for (String line : lines) {
                    if (y < margin) {
                        cs.close();
                        page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER);
                        doc.addPage(page);
                        y = page.getMediaBox().getHeight() - margin;
                        try (var cs2 = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
                            cs2.setFont(font, fontSize);
                            for (String l2 : lines.subList(lines.indexOf(line), lines.size())) {
                                if (y < margin) break;
                                cs2.beginText();
                                cs2.newLineAtOffset(startX, y);
                                cs2.showText(l2);
                                cs2.endText();
                                y -= leading;
                            }
                        }
                        break;
                    } else {
                        cs.beginText();
                        cs.newLineAtOffset(startX, y);
                        cs.showText(line);
                        cs.endText();
                        y -= leading;
                    }
                }
            }

            try (var baos = new java.io.ByteArrayOutputStream()) {
                doc.save(baos);
                return baos.toByteArray();
            }
        }
    }

    private static java.util.List<String> wrapText(String text,
                                                   org.apache.pdfbox.pdmodel.font.PDFont font,
                                                   float fontSize,
                                                   float maxWidth) throws IOException {
        java.util.List<String> result = new java.util.ArrayList<>();
        for (String rawLine : text.split("\r?\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : rawLine.split("(?<=\\s)|(?=\\s)")) {
                String candidate = line + word;
                float w = font.getStringWidth(candidate) / 1000 * fontSize;
                if (w > maxWidth && line.length() > 0) {
                    result.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line.append(word);
                }
            }
            result.add(line.toString());
        }
        return result;
    }
}
