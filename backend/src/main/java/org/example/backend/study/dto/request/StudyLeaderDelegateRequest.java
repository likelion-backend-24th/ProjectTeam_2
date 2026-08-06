package org.example.backend.study.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class StudyLeaderDelegateRequest {
    @NotNull
    private Long newLeaderId;
}
