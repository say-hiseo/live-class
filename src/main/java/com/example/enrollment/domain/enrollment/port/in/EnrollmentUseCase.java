package com.example.enrollment.domain.enrollment.port.in;

import com.example.enrollment.domain.enrollment.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EnrollmentUseCase {

    String requestEnrollment(Long userId, Long courseId);

    EnrollmentResultResponse getEnrollmentResult(String requestId);

    EnrollmentResponse confirmEnrollment(Long userId, Long enrollmentId);

    EnrollmentResponse cancelEnrollment(Long userId, Long enrollmentId, EnrollmentCancelRequest request);

    Page<EnrollmentResponse> getMyEnrollments(Long userId, Pageable pageable);

    EnrollmentResponse getEnrollment(Long userId, Long enrollmentId);
}