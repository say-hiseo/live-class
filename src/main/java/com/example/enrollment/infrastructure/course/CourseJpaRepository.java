package com.example.enrollment.infrastructure.course;

import com.example.enrollment.domain.course.model.Course;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CourseJpaRepository extends JpaRepository<CourseJpaEntity, Long> {

    Page<CourseJpaEntity> findAllByStatus(Course.Status status, Pageable pageable);
    Page<CourseJpaEntity> findAll(Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CourseJpaEntity c WHERE c.id = :id")
    Optional<CourseJpaEntity> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT c FROM CourseJpaEntity c WHERE c.status = 'OPEN' AND c.deadline < :today")
    List<CourseJpaEntity> findOpenCoursesWithExpiredDeadline(@Param("today") LocalDate today);
}