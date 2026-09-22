package io.github.adnansaliq.workmyresume.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * Stores file bytes on disk and returns the storage key.
     * The key is a unique filename inside the upload directory
     * (e.g. "&lt;uuid&gt;-&lt;filename&gt;") and is what gets persisted in the DB.
     */
    public String store(byte[] content, String originalFilename) {
        String safeName = sanitize(originalFilename);
        String fileName = UUID.randomUUID() + "-" + safeName;
        Path target = uploadDir.resolve(fileName).normalize();

        // Defense in depth: resolved path must stay inside the upload directory
        if (!target.startsWith(uploadDir)) {
            throw new IllegalArgumentException("Invalid filename");
        }

        try {
            Files.createDirectories(uploadDir);
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file", e);
        }

        return fileName;
    }

    /** Reads a stored file back by its storage key. */
    public byte[] load(String storageKey) {
        Path target = uploadDir.resolve(storageKey).normalize();

        if (!target.startsWith(uploadDir)) {
            throw new IllegalArgumentException("Invalid storage key");
        }

        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read file", e);
        }
    }

    /** Strips directory components and dangerous characters from a filename. */
    private String sanitize(String name) {
        if (name == null || name.isBlank()) {
            return "file";
        }
        String base = name;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            base = name.substring(slash + 1);
        }
        return base.replaceAll("[^a-zA-Z0-9.\\-_]", "_").toLowerCase(Locale.ROOT);
    }
}