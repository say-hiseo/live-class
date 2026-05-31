package com.example.enrollment.domain.enrollment.dto;

import com.example.enrollment.domain.enrollment.model.Enrollment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EnrollmentResponse {

    private final Long id;
    private final Long courseId;
    private final Long userId;
    private final Enrollment.Status status;
    private final LocalDateTime confirmedAt;
    private final LocalDateTime cancelledAt;
    private final String cancelReason;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static EnrollmentResponse from(Enrollment enrollment) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .courseId(enrollment.getCourseId())
                .userId(enrollment.getUserId())
                .status(enrollment.getStatus())
                .confirmedAt(enrollment.getConfirmedAt())
                .cancelledAt(enrollment.getCancelledAt())
                .cancelReason(enrollment.getCancelReason())
                .createdAt(enrollment.getCreatedAt())
                .updatedAt(enrollment.getUpdatedAt())
                .build();
    }
}