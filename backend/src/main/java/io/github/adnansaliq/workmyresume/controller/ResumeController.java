package io.github.adnansaliq.workmyresume.controller;

import io.github.adnansaliq.workmyresume.domain.Plan;
import io.github.adnansaliq.workmyresume.domain.ResumeFile;
import io.github.adnansaliq.workmyresume.domain.User;
import io.github.adnansaliq.workmyresume.dto.AnalyzeRequest;
import io.github.adnansaliq.workmyresume.dto.CreateResumeRequest;
import io.github.adnansaliq.workmyresume.dto.CreateVersionRequest;
import io.github.adnansaliq.workmyresume.dto.ResumeAnalysisResponse;
import io.github.adnansaliq.workmyresume.dto.ResumeDetailResponse;
import io.github.adnansaliq.workmyresume.dto.ResumeResponse;
import io.github.adnansaliq.workmyresume.dto.ResumeVersionResponse;
import io.github.adnansaliq.workmyresume.repository.UserRepository;
import io.github.adnansaliq.workmyresume.service.AnalysisService;
import io.github.adnansaliq.workmyresume.service.FileStorageService;
import io.github.adnansaliq.workmyresume.service.ResumeService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
public class ResumeController {

    private final UserRepository userRepository;
    private final ResumeService resumeService;
    private final FileStorageService fileStorageService;
    private final AnalysisService analysisService;

    public ResumeController(UserRepository userRepository,
                            ResumeService resumeService,
                            FileStorageService fileStorageService,
                            AnalysisService analysisService) {
        this.userRepository = userRepository;
        this.resumeService = resumeService;
        this.fileStorageService = fileStorageService;
        this.analysisService = analysisService;
    }

    @GetMapping("/resume/analyze")
    @PreAuthorize("hasRole('USER')")
    public String analyzeResume(Authentication authentication) {

        User user = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow();

        if (user.getPlan() == Plan.FREE) {
            return "Free plan: Limited analysis";
        }

        return "Pro plan: Full AI analysis";
    }

    @PostMapping("/resumes")
    public ResponseEntity<ResumeResponse> createResume(
            @Valid @RequestBody CreateResumeRequest request,
            Authentication authentication) {

        ResumeResponse response = resumeService.createResume(authentication.getName(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/resumes")
    public List<ResumeResponse> listResumes(Authentication authentication) {
        return resumeService.getResumesForUser(authentication.getName());
    }

    @GetMapping("/resumes/{resumeId}")
    public ResumeDetailResponse getResumeDetail(
            @PathVariable Long resumeId,
            Authentication authentication) {

        return resumeService.getResumeDetail(authentication.getName(), resumeId);
    }

    @PostMapping("/resumes/{resumeId}/versions")
    public ResponseEntity<ResumeVersionResponse> createVersion(
            @PathVariable Long resumeId,
            @Valid @RequestBody CreateVersionRequest request,
            Authentication authentication) {

        ResumeVersionResponse response =
                resumeService.createVersion(authentication.getName(), resumeId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/resumes/{resumeId}/upload")
    public ResponseEntity<ResumeVersionResponse> uploadResume(
            @PathVariable Long resumeId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            Authentication authentication) throws IOException {

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        ResumeVersionResponse response = resumeService.uploadResumeFile(
                authentication.getName(),
                resumeId,
                title,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getBytes());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/resumes/{resumeId}/versions/{versionId}/file")
    public ResponseEntity<byte[]> downloadResumeFile(
            @PathVariable Long resumeId,
            @PathVariable Long versionId,
            Authentication authentication) {

        ResumeFile file = resumeService.downloadFile(authentication.getName(), resumeId, versionId);
        byte[] content = fileStorageService.load(file.getStorageKey());

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (file.getContentType() != null && !file.getContentType().isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(file.getContentType());
            } catch (InvalidMediaTypeException ignored) {
                // Stored content type may be unreliable; default to octet-stream.
            }
        }

        ContentDisposition disposition =
                ContentDisposition.attachment().filename(file.getOriginalFilename()).build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(content);
    }

    @PostMapping("/resumes/{resumeId}/versions/{versionId}/analyze")
    public ResponseEntity<ResumeAnalysisResponse> analyzeResume(
            @PathVariable Long resumeId,
            @PathVariable Long versionId,
            @RequestBody(required = false) @Valid AnalyzeRequest request,
            Authentication authentication) {

        String jobDescription = request == null ? null : request.getJobDescription();

        ResumeAnalysisResponse response = analysisService.analyzeVersion(
                authentication.getName(), resumeId, versionId, jobDescription);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}