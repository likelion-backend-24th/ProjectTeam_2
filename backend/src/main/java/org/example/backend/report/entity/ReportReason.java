package org.example.backend.report.entity;

import lombok.Getter;

@Getter
public enum ReportReason {
    SPAM("스팸 · 광고"),
    ABUSE("욕설 · 비방"),
    INAPPROPRIATE("부적절한 내용"),
    ETC("기타");

    private final String label;

    ReportReason(String label) {
        this.label = label;
    }
}
