package com.example.enrollment.infrastructure.enrollment;

import com.example.enrollment.domain.enrollment.model.Enrollment;
import com.example.enrollment.global.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EnrollmentJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enrollment.Status status;

    private LocalDateTime confirmedAt;

    private LocalDateTime cancelledAt;

    @Column(length = 255)
    private String cancelReason;

    // JpaEntity → 도메인 모델 변환
    public Enrollment toDomain() {
        return Enrollment.builder()
                .id(this.id)
                .courseId(this.courseId)
                .userId(this.userId)
                .status(this.status)
                .confirmedAt(this.confirmedAt)
                .cancelledAt(this.cancelledAt)
                .cancelReason(this.cancelReason)
                .createdAt(this.getCreatedAt())
                .updatedAt(this.getUpdatedAt())
                .build();
    }

    // 도메인 모델 → JpaEntity 변환
    public static EnrollmentJpaEntity fromDomain(Enrollment enrollment) {
        return EnrollmentJpaEntity.builder()
                .id(enrollment.getId())
                .courseId(enrollment.getCourseId())
                .userId(enrollment.getUserId())
                .status(enrollment.getStatus())
                .confirmedAt(enrollment.getConfirmedAt())
                .cancelledAt(enrollment.getCancelledAt())
                .cancelReason(enrollment.getCancelReason())
                .build();
    }

    // 도메인 상태 동기화
    public void update(Enrollment enrollment) {
        this.status = enrollment.getStatus();
        this.confirmedAt = enrollment.getConfirmedAt();
        this.cancelledAt = enrollment.getCancelledAt();
        this.cancelReason = enrollment.getCancelReason();
    }
}