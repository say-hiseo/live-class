package com.example.enrollment.domain.course.service;

import com.example.enrollment.domain.course.dto.*;
import com.example.enrollment.domain.course.model.Course;
import com.example.enrollment.domain.course.port.in.CourseUseCase;
import com.example.enrollment.domain.course.port.out.CoursePort;
import com.example.enrollment.domain.enrollment.model.Enrollment;
import com.example.enrollment.domain.enrollment.port.out.EnrollmentPort;
import com.example.enrollment.domain.enrollment.port.out.WaitlistPort;
import com.example.enrollment.domain.user.model.User;
import com.example.enrollment.domain.user.port.out.UserPort;
import com.example.enrollment.global.exception.ErrorCode;
import com.example.enrollment.global.exception.RestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CourseService implements CourseUseCase {

    private final CoursePort coursePort;
    private final UserPort userPort;
    private final EnrollmentPort enrollmentPort;
    private final WaitlistPort waitlistPort;

    @Override
    @Transactional
    public CourseResponse createCourse(Long userId, CourseCreateRequest request) {
        User user = getUser(userId);
        if (!user.isCreator()) {
            throw new RestApiException(ErrorCode.NOT_CREATOR);
        }

        validateCourseDates(request.getStartDate(), request.getEndDate(), request.getDeadline());

        Course course = Course.builder()
                .creatorId(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .capacity(request.getCapacity())
                .enrolledCount(0)
                .status(Course.Status.DRAFT)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .deadline(request.getDeadline())
                .build();

        return CourseResponse.from(coursePort.save(course));
    }

    @Override
    @Transactional
    public CourseResponse updateCourse(Long userId, Long courseId, CourseUpdateRequest request) {
        User user = getUser(userId);
        if (!user.isCreator()) {
            throw new RestApiException(ErrorCode.NOT_CREATOR);
        }

        Course course = getCourse(courseId);
        if (!course.isOwnedBy(userId)) {
            throw new RestApiException(ErrorCode.NOT_COURSE_OWNER);
        }
        if (course.getStatus() != Course.Status.DRAFT) {
            throw new RestApiException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        validateCourseDates(request.getStartDate(), request.getEndDate(), request.getDeadline());

        Course updated = Course.builder()
                .id(course.getId())
                .creatorId(course.getCreatorId())
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .capacity(request.getCapacity())
                .enrolledCount(course.getEnrolledCount())
                .status(course.getStatus())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .deadline(request.getDeadline())
                .build();

        return CourseResponse.from(coursePort.save(updated));
    }

    @Override
    @Transactional
    public void deleteCourse(Long userId, Long courseId) {
        User user = getUser(userId);
        if (!user.isCreator()) {
            throw new RestApiException(ErrorCode.NOT_CREATOR);
        }

        Course course = getCourse(courseId);
        if (!course.isOwnedBy(userId)) {
            throw new RestApiException(ErrorCode.NOT_COURSE_OWNER);
        }
        if (!course.isDeletable()) {
            throw new RestApiException(ErrorCode.COURSE_NOT_DELETABLE);
        }

        coursePort.deleteById(courseId);
    }

    @Override
    @Transactional
    public CourseResponse openCourse(Long userId, Long courseId) {
        User user = getUser(userId);
        if (!user.isCreator()) {
            throw new RestApiException(ErrorCode.NOT_CREATOR);
        }

        Course course = getCourse(courseId);
        if (!course.isOwnedBy(userId)) {
            throw new RestApiException(ErrorCode.NOT_COURSE_OWNER);
        }

        course.open();
        return CourseResponse.from(coursePort.save(course));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseResponse> getCourses(Course.Status status, Pageable pageable) {
        return coursePort.findAll(status, pageable)
                .map(CourseResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseDetail(Long courseId) {
        Course course = getCourse(courseId);
        int waitlistCount = waitlistPort.countWaitingByCourseId(courseId);
        return CourseDetailResponse.of(course, waitlistCount);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnrolledStudentResponse> getEnrolledStudents(Long userId, Long courseId, Pageable pageable) {
        User user = getUser(userId);
        if (!user.isCreator()) {
            throw new RestApiException(ErrorCode.NOT_CREATOR);
        }

        Course course = getCourse(courseId);
        if (!course.isOwnedBy(userId)) {
            throw new RestApiException(ErrorCode.NOT_COURSE_OWNER);
        }

        return enrollmentPort.findAllByCourseId(courseId, pageable)
                .map(enrollment -> EnrolledStudentResponse.of(enrollment.getUserId(), enrollment));
    }

    private void validateCourseDates(LocalDate startDate, LocalDate endDate, LocalDate deadline) {
        if (endDate.isBefore(startDate)) {
            throw new RestApiException(ErrorCode.BAD_REQUEST);
        }
        if (deadline.isAfter(endDate)) {
            throw new RestApiException(ErrorCode.BAD_REQUEST);
        }
    }

    private User getUser(Long userId) {
        return userPort.findById(userId)
                .orElseThrow(() -> new RestApiException(ErrorCode.USER_NOT_FOUND));
    }

    private Course getCourse(Long courseId) {
        return coursePort.findById(courseId)
                .orElseThrow(() -> new RestApiException(ErrorCode.COURSE_NOT_FOUND));
    }
}