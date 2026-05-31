package com.example.enrollment.domain.enrollment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EnrollmentCancelRequest {

    private String cancelReason;
}