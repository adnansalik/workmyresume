package io.github.adnansaliq.workmyresume.service;

import io.github.adnansaliq.workmyresume.domain.Resume;
import io.github.adnansaliq.workmyresume.domain.ResumeVersion;
import io.github.adnansaliq.workmyresume.domain.User;
import io.github.adnansaliq.workmyresume.exception.ResourceNotFoundException;
import io.github.adnansaliq.workmyresume.repository.ResumeRepository;
import io.github.adnansaliq.workmyresume.repository.ResumeVersionRepository;
import io.github.adnansaliq.workmyresume.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Reads the authenticated user and their owned resume resources, applying the
 * same 404 semantics everywhere: a missing or not-owned resource looks the
 * same ("Resume not found") so we never reveal which resumes exist.
 */
@Service
public class ResumeAccessService {

    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;

    public ResumeAccessService(UserRepository userRepository,
                               ResumeRepository resumeRepository,
                               ResumeVersionRepository resumeVersionRepository) {
        this.userRepository = userRepository;
        this.resumeRepository = resumeRepository;
        this.resumeVersionRepository = resumeVersionRepository;
    }

    /** Loads the authenticated user, or throws UsernameNotFoundException. */
    public User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    /** Loads a resume the given user owns; otherwise 404. */
    public Resume requireOwnedResume(String email, Long resumeId) {
        User user = requireUser(email);
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));
        if (!resume.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Resume not found");
        }
        return resume;
    }

    /** Loads a version belonging to a resume owned by the user; otherwise 404. */
    public ResumeVersion requireOwnedVersion(String email, Long resumeId, Long versionId) {
        Resume resume = requireOwnedResume(email, resumeId);
        ResumeVersion version = resumeVersionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));
        if (!version.getResume().getId().equals(resume.getId())) {
            throw new ResourceNotFoundException("Resume not found");
        }
        return version;
    }
}