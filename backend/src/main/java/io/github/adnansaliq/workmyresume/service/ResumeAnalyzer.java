package io.github.adnansaliq.workmyresume.service;

/**
 * Produces a resume analysis from extracted resume text.
 * Implementations may be local heuristics or AI-based.
 */
public interface ResumeAnalyzer {

    AnalysisResult analyze(String rawText, String jobDescription);
}