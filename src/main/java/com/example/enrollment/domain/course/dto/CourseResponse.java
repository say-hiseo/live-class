package com.example.enrollment.domain.course.dto;

import com.example.enrollment.domain.course.model.Course;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class CourseResponse {

    private final Long id;
    private final Long creatorId;
    private final String title;
    private final String description;
    private final int price;
    private final int capacity;
    private final int enrolledCount;
    private final Course.Status status;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalDate deadline;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static CourseResponse from(Course course) {
        return CourseResponse.builder()
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
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}