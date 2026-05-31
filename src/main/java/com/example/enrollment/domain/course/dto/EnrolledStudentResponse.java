package com.example.enrollment.domain.course.dto;

import com.example.enrollment.domain.enrollment.model.Enrollment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EnrolledStudentResponse {

    private final Long enrollmentId;
    private final Long userId;
    private final Enrollment.Status status;
    private final LocalDateTime confirmedAt;
    private final LocalDateTime createdAt;

    public static EnrolledStudentResponse of(Long userId, Enrollment enrollment) {
        return EnrolledStudentResponse.builder()
                .enrollmentId(enrollment.getId())
                .userId(userId)
                .status(enrollment.getStatus())
                .confirmedAt(enrollment.getConfirmedAt())
                .createdAt(enrollment.getCreatedAt())
                .build();
    }
}