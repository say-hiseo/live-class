package com.example.enrollment.domain.enrollment.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class Waitlist {

    private final Long id;
    private final Long courseId;
    private final Long userId;
    private int waitOrder;
    private Status status;
    private LocalDateTime promotedAt;
    private LocalDateTime cancelledAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public enum Status {
        WAITING, PROMOTED, CANCELLED
    }

    public void promote() {
        if (this.status != Status.WAITING) {
            throw new IllegalStateException("대기 중인 상태만 승격할 수 있습니다.");
        }
        this.status = Status.PROMOTED;
        this.promotedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (this.status != Status.WAITING) {
            throw new IllegalStateException("대기 중인 상태만 취소할 수 있습니다.");
        }
        this.status = Status.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    public boolean isWaiting() {
        return this.status == Status.WAITING;
    }
}