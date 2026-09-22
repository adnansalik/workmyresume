package io.github.adnansaliq.workmyresume.service;

import io.github.adnansaliq.workmyresume.exception.UnsupportedFileTypeException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextExtractionServiceTest {

    private final TextExtractionService extractor = new TextExtractionService();

    @Test
    void extractsPlainText() {
        String text = extractor.extractText("resume.txt", "text/plain", "hello resume".getBytes());
        assertThat(text).isEqualTo("hello resume");
    }

    @Test
    void extractsPdfContent() throws Exception {
        byte[] pdf;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (PDDocument doc = new PDDocument()) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    cs.newLineAtOffset(50, 700);
                    cs.showText("Jane Smith Software Engineer");
                    cs.endText();
                }
                doc.save(out);
            }
            pdf = out.toByteArray();
        }

        String text = extractor.extractText("resume.pdf", "application/pdf", pdf);
        assertThat(text.toLowerCase()).contains("jane smith");
    }

    @Test
    void extractsDocxContentEvenWithWrongContentType() throws Exception {
        byte[] docx;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (XWPFDocument doc = new XWPFDocument()) {
                XWPFParagraph paragraph = doc.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText("Jane Smith Software Engineer");
                doc.write(out);
            }
            docx = out.toByteArray();
        }

        // Extension fallback must kick in even for a bogus content type.
        String text = extractor.extractText("resume.docx", "application/octet-stream", docx);
        assertThat(text.toLowerCase()).contains("jane smith");
    }

    @Test
    void rejectsUnsupportedFileType() {
        assertThatThrownBy(() ->
                extractor.extractText("photo.png", "image/png", new byte[]{1, 2, 3}))
                .isInstanceOf(UnsupportedFileTypeException.class)
                .hasMessageContaining("photo.png");
    }
}