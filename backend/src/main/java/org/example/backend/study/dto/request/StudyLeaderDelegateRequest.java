package org.example.backend.study.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class StudyLeaderDelegateRequest {
    @Schema(description = "방장을 위임받을 스터디 멤버의 유저 ID", example = "3")
    @NotNull
    private Long newLeaderId;
}
