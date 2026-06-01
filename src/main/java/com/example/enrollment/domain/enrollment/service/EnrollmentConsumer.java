package com.example.enrollment.domain.enrollment.service;

import com.example.enrollment.domain.course.model.Course;
import com.example.enrollment.domain.course.port.out.CoursePort;
import com.example.enrollment.domain.enrollment.dto.EnrollmentQueueRequest;
import com.example.enrollment.domain.enrollment.dto.EnrollmentResultResponse;
import com.example.enrollment.domain.enrollment.model.Enrollment;
import com.example.enrollment.domain.enrollment.port.out.EnrollmentPort;
import com.example.enrollment.global.config.RedisQueueKey;
import com.example.enrollment.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrollmentConsumer {

    private final RedisTemplate<String, String> redisTemplate;
    private final EnrollmentPort enrollmentPort;
    private final CoursePort coursePort;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 100)
    @Transactional
    public void processEnrollmentQueue() {
        String activeSetKey = RedisQueueKey.activeCourseQueues();
        var courseIds = redisTemplate.opsForSet().members(activeSetKey);
        if (courseIds == null || courseIds.isEmpty()) return;

        for (String courseId : courseIds) {
            String queueKey = RedisQueueKey.enrollmentQueue(Long.parseLong(courseId));
            Long queueSize = redisTemplate.opsForList().size(queueKey);

            if (queueSize == null || queueSize == 0) {
                // 큐가 비었으면 Set에서 제거
                redisTemplate.opsForSet().remove(activeSetKey, courseId);
                continue;
            }

            processQueue(queueKey);
        }
    }

    private void processQueue(String queueKey) {
        String json = redisTemplate.opsForList().leftPop(queueKey);
        if (json == null) return;

        EnrollmentQueueRequest request;
        try {
            request = objectMapper.readValue(json, EnrollmentQueueRequest.class);
        } catch (JsonProcessingException e) {
            log.error("큐 요청 파싱 실패: {}", e.getMessage());
            return;
        }

        String resultKey = RedisQueueKey.enrollmentResult(request.getRequestId());

        try {
            Course course = coursePort.findByIdWithLock(request.getCourseId())
                    .orElseThrow(() -> new RuntimeException(ErrorCode.COURSE_NOT_FOUND.getMessage()));

            if (!course.isEnrollable()) {
                saveResult(resultKey, new EnrollmentResultResponse(
                        request.getRequestId(), "FAIL",
                        course.isFull() ? "정원이 초과되었습니다. 대기열에 등록해주세요."
                                : "모집 중인 강의만 신청할 수 있습니다.", null));
                return;
            }

            boolean alreadyEnrolled = enrollmentPort
                    .findActiveByCourseIdAndUserId(request.getCourseId(), request.getUserId())
                    .isPresent();
            if (alreadyEnrolled) {
                saveResult(resultKey, new EnrollmentResultResponse(
                        request.getRequestId(), "FAIL", "이미 신청한 강의입니다.", null));
                return;
            }

            Enrollment enrollment = Enrollment.builder()
                    .courseId(request.getCourseId())
                    .userId(request.getUserId())
                    .status(Enrollment.Status.PENDING)
                    .build();

            Enrollment saved = enrollmentPort.save(enrollment);

            saveResult(resultKey, new EnrollmentResultResponse(
                    request.getRequestId(), "SUCCESS",
                    "수강 신청이 완료됐습니다. 24시간 내 결제를 완료해주세요.", saved.getId()));

            log.info("수강 신청 처리 완료 - userId: {}, courseId: {}, enrollmentId: {}",
                    request.getUserId(), request.getCourseId(), saved.getId());

        } catch (Exception e) {
            log.error("수강 신청 처리 실패 - requestId: {}, error: {}",
                    request.getRequestId(), e.getMessage());
            saveResult(resultKey, new EnrollmentResultResponse(
                    request.getRequestId(), "FAIL", "수강 신청 처리 중 오류가 발생했습니다.", null));
        }
    }

    private void saveResult(String resultKey, EnrollmentResultResponse result) {
        try {
            redisTemplate.opsForValue().set(
                    resultKey,
                    objectMapper.writeValueAsString(result),
                    30, TimeUnit.MINUTES
            );
        } catch (JsonProcessingException e) {
            log.error("결과 저장 실패: {}", e.getMessage());
        }
    }
}