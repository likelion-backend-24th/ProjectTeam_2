package org.example.backend.post.entity;

public enum PostCategory {
    JOB_INFO("취업정보"),
    INTERVIEW_REVIEW("면접후기"),
    RESUME("자소서"),
    FREE("자유");

    private final String label;

    PostCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}