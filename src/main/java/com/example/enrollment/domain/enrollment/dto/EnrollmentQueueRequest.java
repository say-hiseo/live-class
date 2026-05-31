package com.example.enrollment.domain.enrollment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentQueueRequest {

    private String requestId;
    private Long courseId;
    private Long userId;
}