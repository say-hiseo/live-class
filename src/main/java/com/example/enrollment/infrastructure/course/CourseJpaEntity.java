package com.example.enrollment.infrastructure.course;

import com.example.enrollment.domain.course.model.Course;
import com.example.enrollment.global.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CourseJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long creatorId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private int enrolledCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Course.Status status;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private LocalDate deadline;

    // JpaEntity → 도메인 모델 변환
    public Course toDomain() {
        return Course.builder()
                .id(this.id)
                .creatorId(this.creatorId)
                .title(this.title)
                .description(this.description)
                .price(this.price)
                .capacity(this.capacity)
                .enrolledCount(this.enrolledCount)
                .status(this.status)
                .startDate(this.startDate)
                .endDate(this.endDate)
                .deadline(this.deadline)
                .createdAt(this.getCreatedAt())
                .updatedAt(this.getUpdatedAt())
                .build();
    }

    // 도메인 모델 → JpaEntity 변환
    public static CourseJpaEntity fromDomain(Course course) {
        return CourseJpaEntity.builder()
                .id(course.getId())
                .creatorId(course.getCreatorId())
                .title(course.getTitle())
                .description(course.getDescription())
                .price(course.getPrice())
                .capacity(course.getCapacity())
                .enrolledCount(course.getEnrolledCount())
                .status(course.getStatus())
                .startDate(course.getStartDate())
                .endDate(course.getEndDate())
                .deadline(course.getDeadline())
                .build();
    }

    // 도메인 상태 동기화 (영속성 컨텍스트 활용)
    public void update(Course course) {
        this.title = course.getTitle();
        this.description = course.getDescription();
        this.price = course.getPrice();
        this.capacity = course.getCapacity();
        this.enrolledCount = course.getEnrolledCount();
        this.status = course.getStatus();
        this.startDate = course.getStartDate();
        this.endDate = course.getEndDate();
        this.deadline = course.getDeadline();
    }
}