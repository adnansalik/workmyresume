package io.github.adnansaliq.workmyresume.domain;

public enum Plan {
    FREE(3),
    PRO(null);

    /** Analyses allowed per 30-day period; null means unlimited. */
    private final Integer analysisLimit;

    Plan(Integer analysisLimit) {
        this.analysisLimit = analysisLimit;
    }

    public Integer getAnalysisLimit() {
        return analysisLimit;
    }
}