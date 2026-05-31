package com.example.enrollment.infrastructure.enrollment;

import com.example.enrollment.domain.enrollment.model.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EnrollmentJpaRepository extends JpaRepository<EnrollmentJpaEntity, Long> {

    @Query("SELECT e FROM EnrollmentJpaEntity e WHERE e.courseId = :courseId AND e.userId = :userId AND e.status IN ('PENDING', 'CONFIRMED')")
    Optional<EnrollmentJpaEntity> findActiveByCourseIdAndUserId(
            @Param("courseId") Long courseId,
            @Param("userId") Long userId
    );

    Page<EnrollmentJpaEntity> findAllByUserId(Long userId, Pageable pageable);

    Page<EnrollmentJpaEntity> findAllByCourseIdAndStatus(
            Long courseId,
            Enrollment.Status status,
            Pageable pageable
    );

    @Query("SELECT e FROM EnrollmentJpaEntity e WHERE e.status = 'PENDING' AND e.createdAt < :expiredTime")
    List<EnrollmentJpaEntity> findExpiredPendingEnrollments(
            @Param("expiredTime") LocalDateTime expiredTime
    );
}