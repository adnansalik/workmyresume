package io.github.adnansaliq.workmyresume.repository;

import io.github.adnansaliq.workmyresume.domain.ResumeAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysis, Long> {

    List<ResumeAnalysis> findByVersionId(Long versionId);

    @Query("""
            select count(a) from ResumeAnalysis a
            where a.version.resume.user.id = :userId
              and a.createdAt >= :since
            """)
    long countByUserSince(@Param("userId") Long userId, @Param("since") OffsetDateTime since);
}