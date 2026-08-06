package org.example.backend.expert.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.backend.expert.entity.Career;
import org.example.backend.expert.entity.JobField;

@Getter
@Builder
public class CareerResponse {
    private Long id;
    private String companyName;
    private String position;
    private Integer years;
    private JobField jobField;

    public static CareerResponse from(Career career) {
        return CareerResponse.builder()
                .id(career.getId())
                .companyName(career.getCompanyName())
                .position(career.getPosition())
                .years(career.getYears())
                .jobField(career.getJobField())
                .build();
    }
}