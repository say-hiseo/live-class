package com.example.enrollment.domain.enrollment.port.out;

import com.example.enrollment.domain.enrollment.model.Enrollment;

import java.util.List;
import java.util.Optional;

public interface EnrollmentPort {
    Enrollment save(Enrollment enrollment);
    Optional<Enrollment> findById(Long id);
    Optional<Enrollment> findActiveByCourseIdAndUserId(Long courseId, Long userId);
    List<Enrollment> findAllByUserId(Long userId);
    List<Enrollment> findAllByCourseId(Long courseId);
    List<Enrollment> findExpiredPendingEnrollments();
}