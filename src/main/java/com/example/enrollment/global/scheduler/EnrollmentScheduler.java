package com.example.enrollment.global.scheduler;

import com.example.enrollment.domain.enrollment.model.Enrollment;
import com.example.enrollment.domain.enrollment.port.out.EnrollmentPort;
import com.example.enrollment.domain.enrollment.service.WaitlistPromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrollmentScheduler {

    private final EnrollmentPort enrollmentPort;
    private final WaitlistPromotionService waitlistPromotionService;

    // 1분마다 24시간 초과 PENDING 자동 취소
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void cancelExpiredPendingEnrollments() {
        List<Enrollment> expiredEnrollments = enrollmentPort.findExpiredPendingEnrollments();

        for (Enrollment enrollment : expiredEnrollments) {
            try {
                enrollment.cancelByTimeout();
                enrollmentPort.save(enrollment);

                waitlistPromotionService.promoteIfExists(enrollment.getCourseId());

                log.info("PENDING 자동 취소 완료 - enrollmentId: {}, userId: {}",
                        enrollment.getId(), enrollment.getUserId());
            } catch (Exception e) {
                log.error("PENDING 자동 취소 실패 - enrollmentId: {}, error: {}",
                        enrollment.getId(), e.getMessage());
            }
        }
    }
}