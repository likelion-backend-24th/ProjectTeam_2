package org.example.backend.expert.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class ExpertFeedbackListResponse {

    @Schema(description = "받은 문의 스레드 목록")
    private List<FeedbackResponse> feedbacks;

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    private int page;

    @Schema(description = "페이지 크기", example = "20")
    private int size;

    @Schema(description = "전체 요소 개수", example = "12")
    private long totalElements;

    @Schema(description = "전체 페이지 수", example = "1")
    private int totalPages;

    @Schema(description = "다음 페이지 존재 여부", example = "false")
    private boolean hasNext;

    public static ExpertFeedbackListResponse from(Page<FeedbackResponse> page) {
        return ExpertFeedbackListResponse.builder()
                .feedbacks(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }
}