package io.github.adnansaliq.workmyresume.service;

import io.github.adnansaliq.workmyresume.domain.Resume;
import io.github.adnansaliq.workmyresume.domain.ResumeAnalysis;
import io.github.adnansaliq.workmyresume.domain.ResumeVersion;
import io.github.adnansaliq.workmyresume.domain.User;
import io.github.adnansaliq.workmyresume.dto.ResumeAnalysisResponse;
import io.github.adnansaliq.workmyresume.exception.BadRequestException;
import io.github.adnansaliq.workmyresume.exception.ResourceNotFoundException;
import io.github.adnansaliq.workmyresume.repository.ResumeAnalysisRepository;
import io.github.adnansaliq.workmyresume.repository.ResumeRepository;
import io.github.adnansaliq.workmyresume.repository.ResumeVersionRepository;
import io.github.adnansaliq.workmyresume.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisService {

    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final ResumeAnalyzer resumeAnalyzer;

    public AnalysisService(UserRepository userRepository,
                           ResumeRepository resumeRepository,
                           ResumeVersionRepository resumeVersionRepository,
                           ResumeAnalysisRepository resumeAnalysisRepository,
                           ResumeAnalyzer resumeAnalyzer) {
        this.userRepository = userRepository;
        this.resumeRepository = resumeRepository;
        this.resumeVersionRepository = resumeVersionRepository;
        this.resumeAnalysisRepository = resumeAnalysisRepository;
        this.resumeAnalyzer = resumeAnalyzer;
    }

    @Transactional
    public ResumeAnalysisResponse analyzeVersion(String email, Long resumeId, Long versionId,
                                                 String jobDescription) {
        User user = getUserByEmail(email);

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));
        if (!resume.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Resume not found");
        }

        ResumeVersion version = resumeVersionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));
        if (!version.getResume().getId().equals(resume.getId())) {
            throw new ResourceNotFoundException("Resume not found");
        }

        if (version.getRawText() == null || version.getRawText().isBlank()) {
            throw new BadRequestException("Version has no text content to analyze");
        }

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

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
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