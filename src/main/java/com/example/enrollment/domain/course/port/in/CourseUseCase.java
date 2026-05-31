package com.example.enrollment.domain.course.port.in;

import com.example.enrollment.domain.course.dto.*;
import com.example.enrollment.domain.course.model.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseUseCase {

    CourseResponse createCourse(Long userId, CourseCreateRequest request);

    CourseResponse updateCourse(Long userId, Long courseId, CourseUpdateRequest request);

    void deleteCourse(Long userId, Long courseId);

    CourseResponse openCourse(Long userId, Long courseId);

    Page<CourseResponse> getCourses(Course.Status status, Pageable pageable);

    CourseDetailResponse getCourseDetail(Long courseId);

    Page<EnrolledStudentResponse> getEnrolledStudents(Long userId, Long courseId, Pageable pageable);
}