package com.example.enrollment.domain.course.port.out;

import com.example.enrollment.domain.course.model.Course;

import java.util.List;
import java.util.Optional;

public interface CoursePort {
    Course save(Course course);
    Optional<Course> findById(Long id);
    List<Course> findAll(Course.Status status);
    void deleteById(Long id);
    List<Course> findOpenCoursesWithExpiredDeadline();

    Optional<Course> findByIdWithLock(Long id);
}