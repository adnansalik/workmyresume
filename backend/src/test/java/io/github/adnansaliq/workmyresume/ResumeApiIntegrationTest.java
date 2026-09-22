package io.github.adnansaliq.workmyresume;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResumeApiIntegrationTest extends BaseIntegrationTest {

    @Test
    void unauthenticatedResumeCallIsRejected() throws Exception {
        mockMvc.perform(get("/resumes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownershipIsEnforcedOnDetail() throws Exception {
        String tokenA = login(register("resume-owner@example.com").getEmail());
        String tokenB = login(register("resume-other@example.com").getEmail());

        String createBody = mockMvc.perform(post("/resumes")
                        .header("Authorization", BEARER + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Owner CV\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long resumeId = objectMapper.readTree(createBody).path("id").asLong();

        mockMvc.perform(get("/resumes/" + resumeId)
                        .header("Authorization", BEARER + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/resumes/" + resumeId)
                        .header("Authorization", BEARER + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(resumeId));
    }

    @Test
    void uploadTxtExtractsRawText() throws Exception {
        String token = login(register("uploader@example.com").getEmail());

        String createBody = mockMvc.perform(post("/resumes")
                        .header("Authorization", BEARER + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Uploaded CV\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long resumeId = objectMapper.readTree(createBody).path("id").asLong();

        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.txt", "text/plain",
                "Python Java SQL Machine Learning".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/resumes/" + resumeId + "/upload")
                        .file(file)
                        .header("Authorization", BEARER + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rawText").value(containsString("Python")))
                .andExpect(jsonPath("$.versionNumber").value(1));
    }

    @Test
    void analyzeVersionPersistsScore() throws Exception {
        String token = login(register("analyzer@example.com").getEmail());

        String createBody = mockMvc.perform(post("/resumes")
                        .header("Authorization", BEARER + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Dev CV\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long resumeId = objectMapper.readTree(createBody).path("id").asLong();

        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.txt", "text/plain",
                "Python and SQL engineer".getBytes(StandardCharsets.UTF_8));

        String uploadBody = mockMvc.perform(multipart("/resumes/" + resumeId + "/upload")
                        .file(file)
                        .header("Authorization", BEARER + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long versionId = objectMapper.readTree(uploadBody).path("id").asLong();

        mockMvc.perform(post("/resumes/" + resumeId + "/versions/" + versionId + "/analyze")
                        .header("Authorization", BEARER + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobDescription\":\"Need Python, SQL and Machine Learning\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").isNumber())
                .andExpect(jsonPath("$.gaps").isArray());
    }

    @Test
    void analyzingVersionWithoutTextReturns400() throws Exception {
        String token = login(register("empty-cv@example.com").getEmail());

        String createBody = mockMvc.perform(post("/resumes")
                        .header("Authorization", BEARER + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Empty CV\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long resumeId = objectMapper.readTree(createBody).path("id").asLong();

        String versionBody = mockMvc.perform(post("/resumes/" + resumeId + "/versions")
                        .header("Authorization", BEARER + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"no file\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long versionId = objectMapper.readTree(versionBody).path("id").asLong();

        mockMvc.perform(post("/resumes/" + resumeId + "/versions/" + versionId + "/analyze")
                        .header("Authorization", BEARER + token))
                .andExpect(status().isBadRequest());
    }
}