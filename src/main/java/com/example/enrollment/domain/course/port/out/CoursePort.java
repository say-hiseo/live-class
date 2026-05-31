package com.example.enrollment.domain.course.port.out;

import com.example.enrollment.domain.course.model.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CoursePort {
    Course save(Course course);
    Optional<Course> findById(Long id);
    void deleteById(Long id);
    List<Course> findOpenCoursesWithExpiredDeadline();
    Optional<Course> findByIdWithLock(Long id);
    Page<Course> findAll(Course.Status status, Pageable pageable);
}