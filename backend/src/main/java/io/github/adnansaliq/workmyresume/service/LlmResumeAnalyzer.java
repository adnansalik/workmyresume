package io.github.adnansaliq.workmyresume.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * AI-powered analyzer speaking the OpenAI-compatible chat completions API.
 * Falls back to the heuristic analyzer when no API key is configured
 * or when the LLM call fails.
 */
@Component
@Primary
public class LlmResumeAnalyzer implements ResumeAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(LlmResumeAnalyzer.class);

    private final HeuristicResumeAnalyzer fallback;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public LlmResumeAnalyzer(HeuristicResumeAnalyzer fallback,
                             ObjectMapper objectMapper,
                             @Value("${app.ai.api-key:}") String apiKey,
                             @Value("${app.ai.base-url:https://api.openai.com/v1}") String baseUrl,
                             @Value("${app.ai.model:gpt-4o-mini}") String model) {
        this.fallback = fallback;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(60));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public AnalysisResult analyze(String rawText, String jobDescription) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("No app.ai.api-key configured — using the heuristic analyzer");
            return fallback.analyze(rawText, jobDescription);
        }

        try {
            return analyzeWithLlm(rawText, jobDescription);
        } catch (Exception e) {
            log.warn("LLM analysis failed ({}); falling back to the heuristic analyzer", e.getMessage());
            return fallback.analyze(rawText, jobDescription);
        }
    }

    private AnalysisResult analyzeWithLlm(String rawText, String jobDescription) throws Exception {
        String systemPrompt = """
                You are an expert resume reviewer and ATS analyst. Analyze the resume text.
                Return ONLY a JSON object with exactly these keys (no markdown, no commentary):
                "score": integer 0-100,
                "strengths": array of strings,
                "gaps": array of strings,
                "suggestions": array of strings.
                If a job description is provided, assess how well the resume matches it.
                """;

        String userContent = "Resume text:\n" + rawText
                + (jobDescription == null || jobDescription.isBlank()
                        ? ""
                        : "\n\nJob description:\n" + jobDescription);

        ChatRequest payload = new ChatRequest(
                List.of(new ChatMessage("system", systemPrompt),
                        new ChatMessage("user", userContent)),
                model);

        String responseBody = restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .exchange((request, response) -> {
                    if (response.getStatusCode().isError()) {
                        throw new RuntimeException("LLM API error: " + response.getStatusCode());
                    }
                    // Read the raw body directly instead of relying on content-type based
                    // message conversion, which can trip over non-standard content types.
                    try (InputStream in = response.getBody()) {
                        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    }
                });

        return parseAnalysis(responseBody);
    }

    private AnalysisResult parseAnalysis(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String content = root.path("choices").path(0).path("message").path("content").asText();

        // Strip markdown code fences if the model wrapped the JSON in them.
        content = content.replaceAll("(?s)^```(?:json)?\\s*|\\s*```$", "").strip();

        JsonNode analysis = objectMapper.readTree(content);
        int score = Math.max(0, Math.min(100, analysis.path("score").asInt(0)));

        return new AnalysisResult(
                score,
                strings(analysis.path("strengths")),
                strings(analysis.path("gaps")),
                strings(analysis.path("suggestions")));
    }

    private List<String> strings(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(item -> result.add(item.asText()));
        }
        return result;
    }

    private record ChatRequest(List<ChatMessage> messages, String model) {
    }

    private record ChatMessage(String role, String content) {
    }
}