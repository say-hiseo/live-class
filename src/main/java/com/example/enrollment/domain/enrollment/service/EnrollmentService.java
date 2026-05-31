package com.example.enrollment.domain.enrollment.service;

import com.example.enrollment.domain.course.model.Course;
import com.example.enrollment.domain.course.port.out.CoursePort;
import com.example.enrollment.domain.enrollment.dto.*;
import com.example.enrollment.domain.enrollment.model.Enrollment;
import com.example.enrollment.domain.enrollment.model.Waitlist;
import com.example.enrollment.domain.enrollment.port.in.EnrollmentUseCase;
import com.example.enrollment.domain.enrollment.port.out.EnrollmentPort;
import com.example.enrollment.domain.enrollment.port.out.WaitlistPort;
import com.example.enrollment.domain.user.model.User;
import com.example.enrollment.domain.user.port.out.UserPort;
import com.example.enrollment.global.config.RedisQueueKey;
import com.example.enrollment.global.exception.ErrorCode;
import com.example.enrollment.global.exception.RestApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService implements EnrollmentUseCase {

    private final EnrollmentPort enrollmentPort;
    private final CoursePort coursePort;
    private final UserPort userPort;
    private final WaitlistPort waitlistPort;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String requestEnrollment(Long userId, Long courseId) {
        // 사용자/강의 존재 여부 확인하고
        User user = getUser(userId);
        if (!user.isStudent()) {
            throw new RestApiException(ErrorCode.NOT_STUDENT);
        }
        getCourse(courseId);

        // 중복 신청 확인 후
        enrollmentPort.findActiveByCourseIdAndUserId(courseId, userId)
                .ifPresent(e -> { throw new RestApiException(ErrorCode.ALREADY_ENROLLED); });

        // Redis 큐에 요청 적재
        String requestId = UUID.randomUUID().toString();
        EnrollmentQueueRequest queueRequest = new EnrollmentQueueRequest(requestId, courseId, userId);

        try {
            String json = objectMapper.writeValueAsString(queueRequest);
            redisTemplate.opsForList().rightPush(RedisQueueKey.enrollmentQueue(courseId), json);

            EnrollmentResultResponse pending = new EnrollmentResultResponse(
                    requestId, "PENDING", "신청이 접수됐습니다. 잠시 후 결과를 확인해주세요.", null);
            redisTemplate.opsForValue().set(
                    RedisQueueKey.enrollmentResult(requestId),
                    objectMapper.writeValueAsString(pending),
                    30, TimeUnit.MINUTES
            );
        } catch (JsonProcessingException e) {
            log.error("Redis 큐 적재 실패: {}", e.getMessage());
            throw new RestApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return requestId;
    }

    @Override
    public EnrollmentResultResponse getEnrollmentResult(String requestId) {
        String resultJson = redisTemplate.opsForValue().get(RedisQueueKey.enrollmentResult(requestId));
        if (resultJson == null) {
            throw new RestApiException(ErrorCode.NOT_FOUND);
        }
        try {
            return objectMapper.readValue(resultJson, EnrollmentResultResponse.class);
        } catch (JsonProcessingException e) {
            log.error("결과 조회 실패: {}", e.getMessage());
            throw new RestApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public EnrollmentResponse confirmEnrollment(Long userId, Long enrollmentId) {
        User user = getUser(userId);
        if (!user.isStudent()) {
            throw new RestApiException(ErrorCode.NOT_STUDENT);
        }

        Enrollment enrollment = getEnrollment(enrollmentId);
        if (!enrollment.getUserId().equals(userId)) {
            throw new RestApiException(ErrorCode.FORBIDDEN_ACCESS);
        }

        Course course = coursePort.findByIdWithLock(enrollment.getCourseId())
                .orElseThrow(() -> new RestApiException(ErrorCode.COURSE_NOT_FOUND));

        if (course.isFull()) {
            enrollment.cancelByPending("정원이 마감되어 결제가 취소됐습니다.");
            enrollmentPort.save(enrollment);
            throw new RestApiException(ErrorCode.PAYMENT_REJECTED);
        }

        enrollment.confirm();
        course.increaseEnrolledCount();

        enrollmentPort.save(enrollment);
        coursePort.save(course);

        return EnrollmentResponse.from(enrollment);
    }

    @Override
    @Transactional
    public EnrollmentResponse cancelEnrollment(Long userId, Long enrollmentId, EnrollmentCancelRequest request) {
        User user = getUser(userId);
        if (!user.isStudent()) {
            throw new RestApiException(ErrorCode.NOT_STUDENT);
        }

        Enrollment enrollment = getEnrollment(enrollmentId);
        if (!enrollment.getUserId().equals(userId)) {
            throw new RestApiException(ErrorCode.FORBIDDEN_ACCESS);
        }

        if (enrollment.getStatus() == Enrollment.Status.PENDING) {
            enrollment.cancelByPending(request.getCancelReason());
        } else if (enrollment.getStatus() == Enrollment.Status.CONFIRMED) {
            enrollment.cancelByConfirmed(request.getCancelReason());

            Course course = coursePort.findByIdWithLock(enrollment.getCourseId())
                    .orElseThrow(() -> new RestApiException(ErrorCode.COURSE_NOT_FOUND));
            course.decreaseEnrolledCount();
            coursePort.save(course);

            promoteWaitlistIfExists(enrollment.getCourseId());
        } else {
            throw new RestApiException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        enrollmentPort.save(enrollment);
        return EnrollmentResponse.from(enrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> getMyEnrollments(Long userId, Pageable pageable) {
        getUser(userId);
        return enrollmentPort.findAllByUserId(userId, pageable)
                .map(EnrollmentResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollment(Long userId, Long enrollmentId) {
        getUser(userId);
        Enrollment enrollment = getEnrollment(enrollmentId);
        if (!enrollment.getUserId().equals(userId)) {
            throw new RestApiException(ErrorCode.FORBIDDEN_ACCESS);
        }
        return EnrollmentResponse.from(enrollment);
    }

    // 대기자 자동 승격
    private void promoteWaitlistIfExists(Long courseId) {
        waitlistPort.findFirstWaitingByCourseId(courseId).ifPresent(waitlist -> {
            waitlist.promote();
            waitlistPort.save(waitlist);

            Enrollment promoted = Enrollment.builder()
                    .courseId(courseId)
                    .userId(waitlist.getUserId())
                    .status(Enrollment.Status.PENDING)
                    .build();
            enrollmentPort.save(promoted);
            log.info("대기자 승격 완료 - userId: {}, courseId: {}", waitlist.getUserId(), courseId);
        });
    }

    private User getUser(Long userId) {
        return userPort.findById(userId)
                .orElseThrow(() -> new RestApiException(ErrorCode.USER_NOT_FOUND));
    }

    private Course getCourse(Long courseId) {
        return coursePort.findById(courseId)
                .orElseThrow(() -> new RestApiException(ErrorCode.COURSE_NOT_FOUND));
    }

    private Enrollment getEnrollment(Long enrollmentId) {
        return enrollmentPort.findById(enrollmentId)
                .orElseThrow(() -> new RestApiException(ErrorCode.ENROLLMENT_NOT_FOUND));
    }
}