package io.github.adnansaliq.workmyresume.repository;

import io.github.adnansaliq.workmyresume.domain.ResumeVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResumeVersionRepository extends JpaRepository<ResumeVersion, Long> {

    Optional<ResumeVersion> findTopByResumeIdOrderByVersionNumberDesc(Long resumeId);
}