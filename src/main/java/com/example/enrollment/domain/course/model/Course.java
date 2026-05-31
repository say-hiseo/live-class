package com.example.enrollment.domain.course.model;

import com.example.enrollment.domain.user.model.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class Course {

    private final Long id;
    private final Long creatorId;
    private final String title;
    private final String description;
    private final int price;
    private final int capacity;
    private int enrolledCount;
    private Status status;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalDate deadline;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public enum Status {
        DRAFT, OPEN, CLOSED
    }

    public void open() {
        if (this.status != Status.DRAFT) {
            throw new IllegalStateException("초안 상태의 강의만 오픈할 수 있습니다.");
        }
        this.status = Status.OPEN;
    }

    public void close() {
        if (this.status != Status.OPEN) {
            throw new IllegalStateException("모집 중인 강의만 마감할 수 있습니다.");
        }
        this.status = Status.CLOSED;
    }

    public boolean isEnrollable() {
        return this.status == Status.OPEN && !isFull();
    }

    public boolean isWaitlistable() {
        return this.status == Status.OPEN && isFull();
    }

    public boolean isFull() {
        return this.enrolledCount >= this.capacity;
    }

    public boolean isDeletable() {
        return this.status == Status.DRAFT;
    }

    public boolean isOwnedBy(Long userId) {
        return this.creatorId.equals(userId);
    }

    public void increaseEnrolledCount() {
        if (this.enrolledCount >= this.capacity) {
            throw new IllegalStateException("정원이 초과되어 수강 확정이 불가합니다.");
        }
        this.enrolledCount++;
    }

    public void decreaseEnrolledCount() {
        if (this.enrolledCount <= 0) {
            throw new IllegalStateException("수강 인원이 0명 이하로 감소할 수 없습니다.");
        }
        this.enrolledCount--;
    }

    public boolean isDeadlineExpired() {
        return this.deadline != null && LocalDate.now().isAfter(this.deadline);
    }
}