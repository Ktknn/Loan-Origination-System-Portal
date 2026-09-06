package com.example.los.entity.enums;

/**
 * Income range — ported from Salesforce Income_Range__c picklist.
 * getEstimatedMonthlyIncome() dùng để tính DTI trong LoanService.
 */
public enum IncomeRange {
    DUOI_10_TRIEU("dưới 10 triệu",    8_000_000L, 10),
    TU_10_DEN_20_TRIEU("10 - 20 triệu", 15_000_000L, 20),
    TU_20_DEN_30_TRIEU("20 - 30 triệu", 25_000_000L, 30),
    TREN_30_TRIEU("trên 30 triệu",    40_000_000L, 40);

    private final String label;
    private final long   estimatedMonthlyIncome;
    private final int    score;

    IncomeRange(String label, long estimatedMonthlyIncome, int score) {
        this.label = label;
        this.estimatedMonthlyIncome = estimatedMonthlyIncome;
        this.score = score;
    }

    public String getLabel()               { return label; }
    public Long   getEstimatedMonthlyIncome() { return estimatedMonthlyIncome; }
    public int    getScore()               { return score; }

    public static IncomeRange fromLabel(String label) {
        if (label == null || label.isBlank()) return DUOI_10_TRIEU;
        String trimmed = label.trim();
        for (IncomeRange r : values()) {
            if (r.label.equalsIgnoreCase(trimmed) || r.name().equalsIgnoreCase(trimmed)) return r;
        }
        return DUOI_10_TRIEU;
    }
}
