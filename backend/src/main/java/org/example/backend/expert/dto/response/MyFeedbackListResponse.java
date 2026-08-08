package org.example.backend.expert.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MyFeedbackListResponse {

    @Schema(description = "내가 개설한 문의 스레드 요약 목록")
    private List<MyFeedbackSummaryResponse> feedbacks;

    public static MyFeedbackListResponse from(List<MyFeedbackSummaryResponse> feedbacks) {
        return MyFeedbackListResponse.builder().feedbacks(feedbacks).build();
    }
}