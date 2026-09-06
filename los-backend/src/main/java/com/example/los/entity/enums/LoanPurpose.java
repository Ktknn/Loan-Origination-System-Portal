package com.example.los.entity.enums;

public enum LoanPurpose {
    CHUA_BENH("Chữa bệnh",                   30),
    HOC_TAP("Học tập",                     25),
    MUA_PHUONG_TIEN("Mua phương tiện đi lại",    20),
    MUA_SAM_DO_DUNG("Mua sắm đồ dùng sinh hoạt", 20),
    VAY_TIEU_DUNG_KHAC("Vay tiêu dùng khác",   15),
    DU_LICH("Du lịch",                     10);

    private final String label;
    private final int    score;

    LoanPurpose(String label, int score) {
        this.label = label;
        this.score = score;
    }

    public String getLabel() { return label; }
    public int    getScore() { return score; }

    public static LoanPurpose fromLabel(String label) {
        if (label == null || label.isBlank()) return VAY_TIEU_DUNG_KHAC;
        String trimmed = label.trim();
        for (LoanPurpose p : values()) {
            if (p.label.equalsIgnoreCase(trimmed) || p.name().equalsIgnoreCase(trimmed)) return p;
        }
        return VAY_TIEU_DUNG_KHAC;
    }
}
