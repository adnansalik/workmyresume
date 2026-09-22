package io.github.adnansaliq.workmyresume.domain;

public enum Plan {
    FREE(3, 0),
    PRO(null, 39_900); // 399.00 INR

    /** Analyses allowed per 30-day period; null means unlimited. */
    private final Integer analysisLimit;

    /** Price in paise for the checkout flow. */
    private final long amountPaise;

    Plan(Integer analysisLimit, long amountPaise) {
        this.analysisLimit = analysisLimit;
        this.amountPaise = amountPaise;
    }

    public Integer getAnalysisLimit() {
        return analysisLimit;
    }

    public long getAmountPaise() {
        return amountPaise;
    }
}