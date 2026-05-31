package com.example.enrollment.domain.enrollment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResultResponse {

    private String requestId;
    private String status;
    private String message;
    private Long enrollmentId;
}