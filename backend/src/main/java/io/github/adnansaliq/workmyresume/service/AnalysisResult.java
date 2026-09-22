package io.github.adnansaliq.workmyresume.service;

import java.util.List;

public record AnalysisResult(
        int score,
        List<String> strengths,
        List<String> gaps,
        List<String> suggestions
) {
}