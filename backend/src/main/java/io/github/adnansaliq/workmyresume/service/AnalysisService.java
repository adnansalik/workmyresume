package io.github.adnansaliq.workmyresume.service;

import io.github.adnansaliq.workmyresume.domain.Plan;
import io.github.adnansaliq.workmyresume.domain.ResumeAnalysis;
import io.github.adnansaliq.workmyresume.domain.ResumeVersion;
import io.github.adnansaliq.workmyresume.domain.User;
import io.github.adnansaliq.workmyresume.dto.ResumeAnalysisResponse;
import io.github.adnansaliq.workmyresume.exception.BadRequestException;
import io.github.adnansaliq.workmyresume.exception.UsageLimitExceededException;
import io.github.adnansaliq.workmyresume.repository.ResumeAnalysisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AnalysisService {

    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final ResumeAnalyzer resumeAnalyzer;
    private final SubscriptionService subscriptionService;
    private final ResumeAccessService resumeAccessService;

    public AnalysisService(ResumeAnalysisRepository resumeAnalysisRepository,
                           ResumeAnalyzer resumeAnalyzer,
                           SubscriptionService subscriptionService,
                           ResumeAccessService resumeAccessService) {
        this.resumeAnalysisRepository = resumeAnalysisRepository;
        this.resumeAnalyzer = resumeAnalyzer;
        this.subscriptionService = subscriptionService;
        this.resumeAccessService = resumeAccessService;
    }

    @Transactional
    public ResumeAnalysisResponse analyzeVersion(String email, Long resumeId, Long versionId,
                                                 String jobDescription) {
        User user = resumeAccessService.requireUser(email);
        ResumeVersion version = resumeAccessService.requireOwnedVersion(email, resumeId, versionId);

        if (version.getRawText() == null || version.getRawText().isBlank()) {
            throw new BadRequestException("Version has no text content to analyze");
        }

        enforceAnalysisQuota(user);

        AnalysisResult result = resumeAnalyzer.analyze(version.getRawText(), jobDescription);

        ResumeAnalysis analysis = new ResumeAnalysis();
        analysis.setVersion(version);
        analysis.setJobDescription(jobDescription);
        analysis.setScore(result.score());
        analysis.setStrengths(result.strengths());
        analysis.setGaps(result.gaps());
        analysis.setSuggestions(result.suggestions());

        ResumeAnalysis saved = resumeAnalysisRepository.save(analysis);
        return toResponse(saved);
    }

    /**
     * Usage accounting: the effective plan's limit is compared against how many
     * analyses this user actually has in the last 30 days (derived, not stored).
     * PRO has no limit (null).
     */
    private void enforceAnalysisQuota(User user) {
        Plan plan = subscriptionService.effectivePlan(user);
        Integer limit = plan.getAnalysisLimit();
        if (limit == null) {
            return;
        }
        long used = resumeAnalysisRepository.countByUserSince(
                user.getId(), OffsetDateTime.now().minusDays(30));
        if (used >= limit) {
            throw new UsageLimitExceededException(
                    "Monthly analysis limit (" + limit + ") reached on the " + plan + " plan");
        }
    }

    private ResumeAnalysisResponse toResponse(ResumeAnalysis analysis) {
        ResumeAnalysisResponse response = new ResumeAnalysisResponse();
        response.setId(analysis.getId());
        response.setVersionId(analysis.getVersion().getId());
        response.setJobDescription(analysis.getJobDescription());
        response.setScore(analysis.getScore());
        response.setStrengths(analysis.getStrengths());
        response.setGaps(analysis.getGaps());
        response.setSuggestions(analysis.getSuggestions());
        response.setCreatedAt(analysis.getCreatedAt());
        return response;
    }
}