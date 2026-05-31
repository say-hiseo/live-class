package com.example.enrollment.domain.enrollment.dto;

import com.example.enrollment.domain.enrollment.model.Waitlist;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class WaitlistResponse {

    private final Long id;
    private final Long courseId;
    private final Long userId;
    private final int waitOrder;
    private final Waitlist.Status status;
    private final LocalDateTime promotedAt;
    private final LocalDateTime cancelledAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static WaitlistResponse from(Waitlist waitlist) {
        return WaitlistResponse.builder()
                .id(waitlist.getId())
                .courseId(waitlist.getCourseId())
                .userId(waitlist.getUserId())
                .waitOrder(waitlist.getWaitOrder())
                .status(waitlist.getStatus())
                .promotedAt(waitlist.getPromotedAt())
                .cancelledAt(waitlist.getCancelledAt())
                .createdAt(waitlist.getCreatedAt())
                .updatedAt(waitlist.getUpdatedAt())
                .build();
    }
}