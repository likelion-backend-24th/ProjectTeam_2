package org.example.backend.expert.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MyFeedbackListResponse {
    private List<MyFeedbackSummaryResponse> feedbacks;

    public static MyFeedbackListResponse from(List<MyFeedbackSummaryResponse> feedbacks) {
        return MyFeedbackListResponse.builder().feedbacks(feedbacks).build();
    }
}