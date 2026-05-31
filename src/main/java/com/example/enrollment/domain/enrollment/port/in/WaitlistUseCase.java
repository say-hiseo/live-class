package com.example.enrollment.domain.enrollment.port.in;

import com.example.enrollment.domain.enrollment.dto.WaitlistResponse;

public interface WaitlistUseCase {

    WaitlistResponse registerWaitlist(Long userId, Long courseId);

    WaitlistResponse cancelWaitlist(Long userId, Long waitlistId);

    WaitlistResponse getMyWaitlist(Long userId, Long courseId);
}