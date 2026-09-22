package io.github.adnansaliq.workmyresume.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HeuristicResumeAnalyzerTest {

    private final HeuristicResumeAnalyzer analyzer = new HeuristicResumeAnalyzer();

    @Test
    void solidResumeScoresHigherThanWeakResume() {
        AnalysisResult strong = analyzer.analyze(
                longResume("Java, Python, SQL, Docker, AWS, Kubernetes, Git, Spring, "
                        + "React, PostgreSQL, Linux, machine learning, CI/CD", 130), null);
        AnalysisResult weak = analyzer.analyze("hello world this is short", null);

        assertThat(strong.score()).isGreaterThan(weak.score());
    }

    @Test
    void scoreIsAlwaysWithinBounds() {
        AnalysisResult result = analyzer.analyze(longResume("nothing much", 5), null);

        assertThat(result.score()).isBetween(0, 100);
    }

    @Test
    void jobDescriptionDrivesGapsForMissingKeywords() {
        String resume = longResume("Python and SQL only", 10);
        String jd = "We need Java and Kubernetes expertise plus Python";

        AnalysisResult result = analyzer.analyze(resume, jd);

        assertThat(result.gaps()).contains(
                "Missing keyword: java",
                "Missing keyword: kubernetes");
        assertThat(result.strengths()).anyMatch(s -> s.contains("python"));
    }

    @Test
    void missingContactDetailsAreSuggested() {
        AnalysisResult result = analyzer.analyze("Just a resume with Java skills nowhere special", null);

        assertThat(result.suggestions()).anyMatch(s -> s.contains("contact email"));
        assertThat(result.suggestions()).anyMatch(s -> s.contains("phone number"));
    }

    @Test
    void presentEmailIsReportedAsStrength() {
        AnalysisResult result = analyzer.analyze(
                longResume("Java at john.doe@example.com with Python", 20), null);

        assertThat(result.strengths()).contains("Contact email found");
    }

    private String longResume(String body, int words) {
        StringBuilder sb = new StringBuilder(body).append("\n");
        for (int i = 0; i < words; i++) {
            sb.append("achievement-").append(i).append(' ');
        }
        return sb.toString();
    }
}