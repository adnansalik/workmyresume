package io.github.adnansaliq.workmyresume.repository;

import io.github.adnansaliq.workmyresume.domain.ResumeFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeFileRepository extends JpaRepository<ResumeFile, Long> {

    List<ResumeFile> findByVersionId(Long versionId);
}