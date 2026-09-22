package io.github.adnansaliq.workmyresume.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Transparent, dependency-free analyzer used as the default/fallback.
 * Scores 0-100 based on keyword (ATS) coverage plus basic resume-quality signals.
 */
@Component
public class HeuristicResumeAnalyzer implements ResumeAnalyzer {

    private static final List<String> TECH_SKILLS = List.of(
            "java", "python", "javascript", "typescript", "react", "angular", "vue",
            "sql", "postgresql", "mysql", "mongodb", "docker", "kubernetes", "aws",
            "azure", "gcp", "git", "rest", "spring", "machine learning", "ai",
            "pandas", "tensorflow", "pytorch", "linux", "html", "css", "graphql",
            "kafka", "redis", "node.js", "c++", "c#", "go", "rust"
    );

    private static final Pattern EMAIL =
            Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE =
            Pattern.compile("\\+?\\d[\\d.\\s()-]{8,}\\d");

    @Override
    public AnalysisResult analyze(String rawText, String jobDescription) {
        String text = rawText.toLowerCase(Locale.ROOT);

        List<String> foundSkills = findMatchingSkills(text, TECH_SKILLS);
        List<String> targetSkills = targetSkills(text, jobDescription);

        List<String> matched = targetSkills.stream()
                .filter(foundSkills::contains)
                .toList();
        List<String> missing = targetSkills.stream()
                .filter(s -> !foundSkills.contains(s))
                .toList();

        double coverage = targetSkills.isEmpty()
                ? 1.0
                : (double) matched.size() / targetSkills.size();

        int keywordScore = (int) Math.round(coverage * 60);
        int emailBonus = EMAIL.matcher(text).find() ? 15 : 0;
        int phoneBonus = PHONE.matcher(text).find() ? 10 : 0;
        int lengthBonus = rawText.trim().split("\\s+").length >= 120 ? 15 : 0;

        int score = Math.min(100, keywordScore + emailBonus + phoneBonus + lengthBonus);

        List<String> strengths = new ArrayList<>();
        if (emailBonus > 0) strengths.add("Contact email found");
        if (phoneBonus > 0) strengths.add("Phone number found");
        if (lengthBonus > 0) strengths.add("Good level of detail (120+ words)");
        if (!matched.isEmpty()) {
            strengths.add("Strong skill coverage: " + String.join(", ", head(matched, 5)));
        }
        if (strengths.isEmpty()) {
            strengths.add("Resume is minimal — adding content will improve the score");
        }

        List<String> gaps = head(missing, 5).stream()
                .map(s -> "Missing keyword: " + s)
                .toList();

        List<String> suggestions = new ArrayList<>();
        if (!missing.isEmpty()) {
            suggestions.add("Add missing keywords seen in the target: "
                    + String.join(", ", head(missing, 5)));
        }
        suggestions.add("Quantify achievements with numbers (e.g. 'reduced latency by 40%')");
        if (emailBonus == 0) suggestions.add("Add a contact email");
        if (phoneBonus == 0) suggestions.add("Add a phone number");

        return new AnalysisResult(score, strengths, gaps, suggestions);
    }

    /** If a job description is given, target only skills that appear in it. */
    private List<String> targetSkills(String text, String jobDescription) {
        if (jobDescription == null || jobDescription.isBlank()) {
            return TECH_SKILLS;
        }
        String jd = jobDescription.toLowerCase(Locale.ROOT);
        return TECH_SKILLS.stream()
                .filter(skill -> containsWholeWord(jd, skill))
                .toList();
    }

    private List<String> findMatchingSkills(String text, List<String> skills) {
        return skills.stream()
                .filter(skill -> containsWholeWord(text, skill))
                .toList();
    }

    private boolean containsWholeWord(String text, String skill) {
        return Pattern.compile("(?i)(?<![a-z0-9])" + Pattern.quote(skill) + "(?![a-z0-9])")
                .matcher(text)
                .find();
    }

    private List<String> head(List<String> list, int n) {
        return list.stream().limit(n).toList();
    }
}