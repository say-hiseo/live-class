package com.example.enrollment.domain.enrollment.port.out;

import com.example.enrollment.domain.enrollment.model.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface EnrollmentPort {
    Enrollment save(Enrollment enrollment);
    Optional<Enrollment> findById(Long id);
    Optional<Enrollment> findActiveByCourseIdAndUserId(Long courseId, Long userId);
    Page<Enrollment> findAllByUserId(Long userId, Pageable pageable);
    List<Enrollment> findExpiredPendingEnrollments();
    Page<Enrollment> findAllByCourseId(Long courseId, Pageable pageable);
}