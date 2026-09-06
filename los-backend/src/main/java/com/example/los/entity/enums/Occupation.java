package com.example.los.entity.enums;

public enum Occupation {
    CAN_BO_CONG_CHUC("Cán bộ / Công chức", 30),
    HUU_TRI("Hưu trí",             25),
    NHAN_VIEN_CONG_TY("Nhân viên công ty", 20),
    KINH_DOANH_TU_DO("Kinh doanh tự do",  15),
    SINH_VIEN("Sinh viên",           10),
    KHAC("Khác",                   5);

    private final String label;
    private final int    score;

    Occupation(String label, int score) {
        this.label = label;
        this.score = score;
    }

    public String getLabel() { return label; }
    public int    getScore() { return score; }

    public static Occupation fromLabel(String label) {
        if (label == null || label.isBlank()) return KHAC;
        String trimmed = label.trim();
        for (Occupation o : values()) {
            if (o.label.equalsIgnoreCase(trimmed) || o.name().equalsIgnoreCase(trimmed)) return o;
        }
        if (trimmed.equalsIgnoreCase("Lao động tự do")) return KINH_DOANH_TU_DO;
        return KHAC;
    }
}
