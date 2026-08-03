package org.example.backend.expert.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class ExpertRejectRequest {
    private String reason;
}