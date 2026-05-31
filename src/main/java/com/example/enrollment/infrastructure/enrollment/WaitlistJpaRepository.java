package com.example.enrollment.infrastructure.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WaitlistJpaRepository extends JpaRepository<WaitlistJpaEntity, Long> {

    @Query("SELECT w FROM WaitlistJpaEntity w WHERE w.courseId = :courseId AND w.userId = :userId AND w.status = 'WAITING'")
    Optional<WaitlistJpaEntity> findWaitingByCourseIdAndUserId(
            @Param("courseId") Long courseId,
            @Param("userId") Long userId
    );

    @Query("SELECT w FROM WaitlistJpaEntity w WHERE w.courseId = :courseId AND w.status = 'WAITING' ORDER BY w.waitOrder ASC")
    Optional<WaitlistJpaEntity> findFirstWaitingByCourseId(
            @Param("courseId") Long courseId
    );

    @Query("SELECT w FROM WaitlistJpaEntity w WHERE w.courseId = :courseId AND w.status = 'WAITING' ORDER BY w.waitOrder ASC")
    List<WaitlistJpaEntity> findAllWaitingByCourseId(
            @Param("courseId") Long courseId
    );

    @Query("SELECT COUNT(w) FROM WaitlistJpaEntity w WHERE w.courseId = :courseId AND w.status = 'WAITING'")
    int countWaitingByCourseId(
            @Param("courseId") Long courseId
    );

    @Query("SELECT w FROM WaitlistJpaEntity w WHERE w.courseId = :courseId AND w.status = 'WAITING'")
    List<WaitlistJpaEntity> findAllWaitingByCourseIdForCancel(
            @Param("courseId") Long courseId
    );
}