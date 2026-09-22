package io.github.adnansaliq.workmyresume.service;

import io.github.adnansaliq.workmyresume.domain.Resume;
import io.github.adnansaliq.workmyresume.domain.ResumeFile;
import io.github.adnansaliq.workmyresume.domain.ResumeVersion;
import io.github.adnansaliq.workmyresume.domain.User;
import io.github.adnansaliq.workmyresume.dto.CreateResumeRequest;
import io.github.adnansaliq.workmyresume.dto.CreateVersionRequest;
import io.github.adnansaliq.workmyresume.dto.ResumeDetailResponse;
import io.github.adnansaliq.workmyresume.dto.ResumeFileResponse;
import io.github.adnansaliq.workmyresume.dto.ResumeResponse;
import io.github.adnansaliq.workmyresume.dto.ResumeVersionResponse;
import io.github.adnansaliq.workmyresume.exception.ResourceNotFoundException;
import io.github.adnansaliq.workmyresume.repository.ResumeRepository;
import io.github.adnansaliq.workmyresume.repository.ResumeVersionRepository;
import io.github.adnansaliq.workmyresume.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ResumeService {

    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final FileStorageService fileStorageService;
    private final TextExtractionService textExtractionService;

    public ResumeService(UserRepository userRepository,
                         ResumeRepository resumeRepository,
                         ResumeVersionRepository resumeVersionRepository,
                         FileStorageService fileStorageService,
                         TextExtractionService textExtractionService) {
        this.userRepository = userRepository;
        this.resumeRepository = resumeRepository;
        this.resumeVersionRepository = resumeVersionRepository;
        this.fileStorageService = fileStorageService;
        this.textExtractionService = textExtractionService;
    }

    @Transactional
    public ResumeResponse createResume(String email, CreateResumeRequest request) {
        User user = getUserByEmail(email);

        Resume resume = new Resume();
        resume.setUser(user);
        String title = request.getTitle();
        resume.setTitle(title == null || title.isBlank() ? "Untitled Resume" : title);

        Resume saved = resumeRepository.save(resume);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ResumeResponse> getResumesForUser(String email) {
        User user = getUserByEmail(email);

        return resumeRepository.findByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumeDetailResponse getResumeDetail(String email, Long resumeId) {
        User user = getUserByEmail(email);

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));

        if (!resume.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Resume not found");
        }

        ResumeDetailResponse detail = new ResumeDetailResponse();
        detail.setId(resume.getId());
        detail.setTitle(resume.getTitle());
        detail.setCreatedAt(resume.getCreatedAt());
        detail.setVersions(resume.getVersions().stream()
                .map(this::toVersionResponse)
                .toList());
        return detail;
    }

    @Transactional
    public ResumeVersionResponse createVersion(String email, Long resumeId, CreateVersionRequest request) {
        User user = getUserByEmail(email);

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));

        // Same 404 whether the resume is missing or owned by someone else,
        // so we don't reveal which resumes exist.
        if (!resume.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Resume not found");
        }

        int nextVersionNumber = resumeVersionRepository
                .findTopByResumeIdOrderByVersionNumberDesc(resumeId)
                .map(ResumeVersion::getVersionNumber)
                .map(n -> n + 1)
                .orElse(1);

        ResumeVersion version = new ResumeVersion();
        version.setResume(resume);
        version.setVersionNumber(nextVersionNumber(resumeId));
        String title = request.getTitle();
        version.setTitle(title == null || title.isBlank() ? resume.getTitle() : title);

        ResumeVersion saved = resumeVersionRepository.save(version);
        return toVersionResponse(saved);
    }

    @Transactional
    public ResumeVersionResponse uploadResumeFile(String email, Long resumeId, String title,
                                                  String originalFilename, String contentType,
                                                  long sizeBytes, byte[] content) {
        User user = getUserByEmail(email);

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));

        if (!resume.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Resume not found");
        }

        // Extract first: validates the file type before anything touches disk.
        String rawText = textExtractionService.extractText(originalFilename, contentType, content);

        String storageKey = fileStorageService.store(content, originalFilename);

        ResumeVersion version = new ResumeVersion();
        version.setResume(resume);
        version.setVersionNumber(nextVersionNumber(resumeId));
        version.setTitle(resolveTitle(title, originalFilename, resume.getTitle()));
        version.setRawText(rawText);

        ResumeFile file = new ResumeFile();
        file.setVersion(version);
        file.setOriginalFilename(originalFilename);
        file.setContentType(contentType);
        file.setSizeBytes(sizeBytes);
        file.setStorageKey(storageKey);
        version.getFiles().add(file);

        ResumeVersion saved = resumeVersionRepository.save(version);
        return toVersionResponse(saved);
    }

    @Transactional(readOnly = true)
    public ResumeFile downloadFile(String email, Long resumeId, Long versionId) {
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

        return version.getFiles().stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No file found for this version"));
    }

    private int nextVersionNumber(Long resumeId) {
        return resumeVersionRepository
                .findTopByResumeIdOrderByVersionNumberDesc(resumeId)
                .map(ResumeVersion::getVersionNumber)
                .map(n -> n + 1)
                .orElse(1);
    }

    private String resolveTitle(String provided, String originalFilename, String fallback) {
        if (provided != null && !provided.isBlank()) {
            return provided;
        }
        if (originalFilename != null && !originalFilename.isBlank()) {
            int dot = originalFilename.lastIndexOf('.');
            String base = dot > 0 ? originalFilename.substring(0, dot) : originalFilename;
            if (!base.isBlank()) {
                return base;
            }
        }
        return (fallback != null && !fallback.isBlank()) ? fallback : "Untitled Resume";
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private ResumeResponse toResponse(Resume resume) {
        ResumeResponse response = new ResumeResponse();
        response.setId(resume.getId());
        response.setTitle(resume.getTitle());
        response.setCreatedAt(resume.getCreatedAt());
        response.setVersionCount(resume.getVersions().size());
        return response;
    }

    private ResumeVersionResponse toVersionResponse(ResumeVersion version) {
        ResumeVersionResponse response = new ResumeVersionResponse();
        response.setId(version.getId());
        response.setResumeId(version.getResume().getId());
        response.setVersionNumber(version.getVersionNumber());
        response.setTitle(version.getTitle());
        response.setRawText(version.getRawText());
        response.setCreatedAt(version.getCreatedAt());
        response.setFiles(version.getFiles().stream()
                .map(this::toFileResponse)
                .toList());
        return response;
    }

    private ResumeFileResponse toFileResponse(ResumeFile file) {
        ResumeFileResponse response = new ResumeFileResponse();
        response.setId(file.getId());
        response.setOriginalFilename(file.getOriginalFilename());
        response.setContentType(file.getContentType());
        response.setSizeBytes(file.getSizeBytes());
        response.setUploadedAt(file.getUploadedAt());
        return response;
    }
}