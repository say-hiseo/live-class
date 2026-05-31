package com.example.enrollment.domain.enrollment.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class Enrollment {

    private final Long id;
    private final Long courseId;
    private final Long userId;
    private Status status;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public enum Status {
        PENDING, CONFIRMED, CANCELLED
    }

    public void confirm() {
        if (this.status != Status.PENDING) {
            throw new IllegalStateException("대기 중인 신청만 확정할 수 있습니다.");
        }
        this.status = Status.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    public void cancelByPending(String reason) {
        if (this.status != Status.PENDING) {
            throw new IllegalStateException("대기 중인 신청만 취소할 수 있습니다.");
        }
        this.status = Status.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelReason = reason;
    }

    public void cancelByConfirmed(String reason) {
        if (this.status != Status.CONFIRMED) {
            throw new IllegalStateException("확정된 신청만 취소할 수 있습니다.");
        }
        if (!isCancellable()) {
            throw new IllegalStateException("결제 확정 후 7일이 초과되어 취소가 불가합니다.");
        }
        this.status = Status.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelReason = reason;
    }

    public void cancelByTimeout() {
        if (this.status != Status.PENDING) {
            throw new IllegalStateException("대기 중인 신청만 자동 취소할 수 있습니다.");
        }
        this.status = Status.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelReason = "24시간 내 미결제로 인한 자동 취소";
    }

    public boolean isCancellable() {
        if (this.confirmedAt == null) return false;
        return LocalDateTime.now().isBefore(this.confirmedAt.plusDays(7));
    }

    public boolean isExpired() {
        if (this.status != Status.PENDING) return false;
        return LocalDateTime.now().isAfter(this.createdAt.plusHours(24));
    }

    public boolean isActive() {
        return this.status == Status.PENDING || this.status == Status.CONFIRMED;
    }
}