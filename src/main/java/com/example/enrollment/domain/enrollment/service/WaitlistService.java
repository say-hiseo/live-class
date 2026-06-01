package com.example.enrollment.domain.enrollment.service;

import com.example.enrollment.domain.course.model.Course;
import com.example.enrollment.domain.course.port.out.CoursePort;
import com.example.enrollment.domain.enrollment.dto.WaitlistResponse;
import com.example.enrollment.domain.enrollment.model.Waitlist;
import com.example.enrollment.domain.enrollment.port.in.WaitlistUseCase;
import com.example.enrollment.domain.enrollment.port.out.EnrollmentPort;
import com.example.enrollment.domain.enrollment.port.out.WaitlistPort;
import com.example.enrollment.domain.user.model.User;
import com.example.enrollment.domain.user.port.out.UserPort;
import com.example.enrollment.global.exception.ErrorCode;
import com.example.enrollment.global.exception.RestApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaitlistService implements WaitlistUseCase {

    private final WaitlistPort waitlistPort;
    private final CoursePort coursePort;
    private final UserPort userPort;
    private final EnrollmentPort enrollmentPort;

    @Override
    @Transactional
    public WaitlistResponse registerWaitlist(Long userId, Long courseId) {
        User user = getUser(userId);
        if (!user.isStudent()) {
            throw new RestApiException(ErrorCode.NOT_STUDENT);
        }

        Course course = getCourse(courseId);

        if (course.getStatus() != Course.Status.OPEN) {
            throw new RestApiException(ErrorCode.COURSE_NOT_OPEN);
        }

        if (!course.isFull()) {
            throw new RestApiException(ErrorCode.BAD_REQUEST);
        }

        enrollmentPort.findActiveByCourseIdAndUserId(courseId, userId)
                .ifPresent(e -> { throw new RestApiException(ErrorCode.ALREADY_ENROLLED); });

        waitlistPort.findWaitingByCourseIdAndUserId(courseId, userId)
                .ifPresent(w -> { throw new RestApiException(ErrorCode.ALREADY_WAITLISTED); });

        int nextOrder = waitlistPort.countWaitingByCourseId(courseId) + 1;

        Waitlist waitlist = Waitlist.builder()
                .courseId(courseId)
                .userId(userId)
                .waitOrder(nextOrder)
                .status(Waitlist.Status.WAITING)
                .build();

        return WaitlistResponse.from(waitlistPort.save(waitlist));
    }

    @Override
    @Transactional
    public WaitlistResponse cancelWaitlist(Long userId, Long waitlistId) {
        User user = getUser(userId);
        if (!user.isStudent()) {
            throw new RestApiException(ErrorCode.NOT_STUDENT);
        }

        Waitlist waitlist = waitlistPort.findById(waitlistId)
                .orElseThrow(() -> new RestApiException(ErrorCode.WAITLIST_NOT_FOUND));

        if (!waitlist.getUserId().equals(userId)) {
            throw new RestApiException(ErrorCode.FORBIDDEN_ACCESS);
        }

        try {
            waitlist.cancel();
        } catch (IllegalStateException e) {
            throw new RestApiException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        return WaitlistResponse.from(waitlistPort.save(waitlist));
    }

    @Override
    @Transactional(readOnly = true)
    public WaitlistResponse getMyWaitlist(Long userId, Long courseId) {
        getUser(userId);
        getCourse(courseId);

        Waitlist waitlist = waitlistPort.findWaitingByCourseIdAndUserId(courseId, userId)
                .orElseThrow(() -> new RestApiException(ErrorCode.WAITLIST_NOT_FOUND));

        return WaitlistResponse.from(waitlist);
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