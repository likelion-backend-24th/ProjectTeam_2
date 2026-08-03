package org.example.backend.expert.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FeedbackCreateRequest {
    private Long expertProfileId;
    private String content;
}