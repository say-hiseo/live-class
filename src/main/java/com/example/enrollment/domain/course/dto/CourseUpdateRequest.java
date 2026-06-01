package com.example.enrollment.domain.course.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseUpdateRequest {

    @NotBlank(message = "강의 제목은 필수입니다.")
    @Size(max = 100, message = "강의 제목은 100자 이내여야 합니다.")
    private String title;

    private String description;

    @NotNull(message = "가격은 필수입니다.")
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    private Integer price;

    @NotNull(message = "정원은 필수입니다.")
    @Min(value = 1, message = "정원은 1명 이상이어야 합니다.")
    private Integer capacity;

    @NotNull(message = "수강 시작일은 필수입니다.")
    private LocalDate startDate;

    @NotNull(message = "수강 종료일은 필수입니다.")
    private LocalDate endDate;

    @NotNull(message = "모집 마감일은 필수입니다.")
    private LocalDate deadline;
}