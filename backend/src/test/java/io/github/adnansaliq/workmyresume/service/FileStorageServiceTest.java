package io.github.adnansaliq.workmyresume.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService storage() {
        return new FileStorageService(tempDir.toString());
    }

    @Test
    void storeAndLoadRoundTrip() {
        byte[] original = "hello résumé".getBytes(StandardCharsets.UTF_8);
        String key = storage().store(original, "CV.pdf");

        assertThat(storage().load(key)).isEqualTo(original);
    }

    @Test
    void sanitizesHostileFilenames() {
        String key = storage().store(new byte[]{1}, "../../evil script.sh");

        assertThat(key).doesNotContain("/")
                .doesNotContain("\\")
                .endsWith("evil_script.sh");

        assertThat(Files.exists(tempDir.resolve(key))).isTrue();
    }

    @Test
    void rejectsTraversalKeysOnLoad() {
        assertThatThrownBy(() -> storage().load("../outside.txt"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage().load("../../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}