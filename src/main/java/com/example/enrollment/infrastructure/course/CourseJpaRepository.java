package com.example.enrollment.infrastructure.course;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CourseJpaRepository extends JpaRepository<CourseJpaEntity, Long> {

    // 상태 필터 조회
    List<CourseJpaEntity> findAllByStatus(Course.Status status);

    // 비관적 락 적용 (동시성 제어)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CourseJpaEntity c WHERE c.id = :id")
    Optional<CourseJpaEntity> findByIdWithLock(@Param("id") Long id);

    // deadline 초과된 OPEN 강의 조회 (스케줄러용)
    @Query("SELECT c FROM CourseJpaEntity c WHERE c.status = 'OPEN' AND c.deadline < :today")
    List<CourseJpaEntity> findOpenCoursesWithExpiredDeadline(@Param("today") LocalDate today);
}