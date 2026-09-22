package io.github.adnansaliq.workmyresume.service;

import io.github.adnansaliq.workmyresume.exception.UnsupportedFileTypeException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Service
public class TextExtractionService {

    public String extractText(String originalFilename, String contentType, byte[] content) {
        String name = originalFilename == null ? "" : originalFilename.toLowerCase(Locale.ROOT);

        if (name.endsWith(".pdf") || isContentType(contentType, "application/pdf")) {
            return extractFromPdf(content);
        }
        if (name.endsWith(".docx") || isContentType(contentType, "vnd.openxmlformats-officedocument.wordprocessingml")) {
            return extractFromDocx(content);
        }
        if (name.endsWith(".txt") || isContentType(contentType, "text/plain")) {
            return new String(content, StandardCharsets.UTF_8);
        }

        throw new UnsupportedFileTypeException("Unsupported file type: " + originalFilename);
    }

    private String extractFromPdf(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to extract text from PDF", e);
        }
    }

    private String extractFromDocx(byte[] content) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content))) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : paragraphs) {
                if (!paragraph.getText().isBlank()) {
                    text.append(paragraph.getText()).append("\n");
                }
            }
            return text.toString().strip();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to extract text from DOCX", e);
        }
    }

    private boolean isContentType(String contentType, String expected) {
        return contentType != null && contentType.toLowerCase(Locale.ROOT).contains(expected);
    }
}