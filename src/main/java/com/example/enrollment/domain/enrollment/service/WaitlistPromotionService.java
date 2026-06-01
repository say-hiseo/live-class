package com.example.enrollment.domain.enrollment.service;

import com.example.enrollment.domain.enrollment.model.Enrollment;
import com.example.enrollment.domain.enrollment.port.out.EnrollmentPort;
import com.example.enrollment.domain.enrollment.port.out.WaitlistPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaitlistPromotionService {

    private final WaitlistPort waitlistPort;
    private final EnrollmentPort enrollmentPort;

    public void promoteIfExists(Long courseId) {
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
}