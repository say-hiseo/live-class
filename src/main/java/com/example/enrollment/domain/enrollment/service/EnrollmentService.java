package com.example.enrollment.domain.enrollment.service;

import com.example.enrollment.domain.course.model.Course;
import com.example.enrollment.domain.course.port.out.CoursePort;
import com.example.enrollment.domain.enrollment.dto.*;
import com.example.enrollment.domain.enrollment.model.Enrollment;
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
    private final WaitlistPromotionService waitlistPromotionService;

    @Override
    public String requestEnrollment(Long userId, Long courseId) {
        User user = getUser(userId);
        if (!user.isStudent()) {
            throw new RestApiException(ErrorCode.NOT_STUDENT);
        }
        getCourse(courseId);

        enrollmentPort.findActiveByCourseIdAndUserId(courseId, userId)
                .ifPresent(e -> { throw new RestApiException(ErrorCode.ALREADY_ENROLLED); });

        String requestId = UUID.randomUUID().toString();
        EnrollmentQueueRequest queueRequest = new EnrollmentQueueRequest(requestId, courseId, userId);

        try {
            String json = objectMapper.writeValueAsString(queueRequest);
            String queueKey = RedisQueueKey.enrollmentQueue(courseId);
            String resultKey = RedisQueueKey.enrollmentResult(requestId);

            EnrollmentResultResponse pending = new EnrollmentResultResponse(
                    requestId, "PENDING", "신청이 접수됐습니다. 잠시 후 결과를 확인해주세요.", null);
            String pendingJson = objectMapper.writeValueAsString(pending);

            try {
                redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                    byte[] queueKeyBytes = queueKey.getBytes();
                    byte[] resultKeyBytes = resultKey.getBytes();
                    byte[] jsonBytes = json.getBytes();
                    byte[] pendingBytes = pendingJson.getBytes();

                    connection.listCommands().rPush(queueKeyBytes, jsonBytes);
                    connection.stringCommands().setEx(resultKeyBytes, 1800L, pendingBytes);
                    return null;
                });
            } catch (Exception pipelineEx) {
                log.warn("파이프라이닝 실패, 개별 명령으로 폴백: {}", pipelineEx.getMessage());
                redisTemplate.opsForList().rightPush(queueKey, json);
                redisTemplate.opsForValue().set(resultKey, pendingJson,
                        java.time.Duration.ofMinutes(30));
            }

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
            try {
                enrollment.cancelByConfirmed(request.getCancelReason());
            } catch (IllegalStateException e) {
                throw new RestApiException(ErrorCode.CANCEL_PERIOD_EXPIRED);
            }

            Course course = coursePort.findByIdWithLock(enrollment.getCourseId())
                    .orElseThrow(() -> new RestApiException(ErrorCode.COURSE_NOT_FOUND));
            course.decreaseEnrolledCount();
            coursePort.save(course);

            waitlistPromotionService.promoteIfExists(enrollment.getCourseId());
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