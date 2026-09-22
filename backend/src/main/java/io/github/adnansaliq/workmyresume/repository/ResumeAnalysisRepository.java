package io.github.adnansaliq.workmyresume.repository;

import io.github.adnansaliq.workmyresume.domain.ResumeAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysis, Long> {

    List<ResumeAnalysis> findByVersionId(Long versionId);
}