package com.example.enrollment.infrastructure.enrollment;

import com.example.enrollment.domain.enrollment.model.Waitlist;
import com.example.enrollment.global.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "waitlists")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WaitlistJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int waitOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Waitlist.Status status;

    private LocalDateTime promotedAt;

    private LocalDateTime cancelledAt;

    // JpaEntity → 도메인 모델 변환
    public Waitlist toDomain() {
        return Waitlist.builder()
                .id(this.id)
                .courseId(this.courseId)
                .userId(this.userId)
                .waitOrder(this.waitOrder)
                .status(this.status)
                .promotedAt(this.promotedAt)
                .cancelledAt(this.cancelledAt)
                .createdAt(this.getCreatedAt())
                .updatedAt(this.getUpdatedAt())
                .build();
    }

    // 도메인 모델 → JpaEntity 변환
    public static WaitlistJpaEntity fromDomain(Waitlist waitlist) {
        return WaitlistJpaEntity.builder()
                .id(waitlist.getId())
                .courseId(waitlist.getCourseId())
                .userId(waitlist.getUserId())
                .waitOrder(waitlist.getWaitOrder())
                .status(waitlist.getStatus())
                .promotedAt(waitlist.getPromotedAt())
                .cancelledAt(waitlist.getCancelledAt())
                .build();
    }

    // 도메인 상태 동기화
    public void update(Waitlist waitlist) {
        this.status = waitlist.getStatus();
        this.promotedAt = waitlist.getPromotedAt();
        this.cancelledAt = waitlist.getCancelledAt();
    }
}