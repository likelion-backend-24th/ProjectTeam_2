package org.example.backend.expert.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * "내 문의 스레드 목록"(GET /api/feedbacks/me) 응답 바디.
 */

@Getter
@Builder
public class MyFeedbackListResponse {
    private List<MyFeedbackSummaryResponse> feedbacks;

    public static MyFeedbackListResponse from(List<MyFeedbackSummaryResponse> feedbacks) {
        return MyFeedbackListResponse.builder().feedbacks(feedbacks).build();
    }
}