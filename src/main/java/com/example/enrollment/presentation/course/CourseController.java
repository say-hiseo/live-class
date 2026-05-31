package com.example.enrollment.presentation.course;

import com.example.enrollment.domain.course.dto.*;
import com.example.enrollment.domain.course.model.Course;
import com.example.enrollment.domain.course.port.in.CourseUseCase;
import com.example.enrollment.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Course", description = "강의 관리 API")
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseUseCase courseUseCase;

    @Operation(summary = "강의 등록", description = "CREATOR만 강의를 등록할 수 있습니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
            @Parameter(description = "사용자 ID") @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CourseCreateRequest request
    ) {
        CourseResponse response = courseUseCase.createCourse(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("강의가 등록됐습니다.", response));
    }

    @Operation(summary = "강의 수정", description = "DRAFT 상태의 강의만 수정 가능합니다.")
    @PutMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(
            @Parameter(description = "사용자 ID") @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long courseId,
            @Valid @RequestBody CourseUpdateRequest request
    ) {
        CourseResponse response = courseUseCase.updateCourse(userId, courseId, request);
        return ResponseEntity.ok(ApiResponse.success("강의가 수정됐습니다.", response));
    }

    @Operation(summary = "강의 삭제", description = "DRAFT 상태의 강의만 삭제 가능합니다.")
    @DeleteMapping("/{courseId}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(
            @Parameter(description = "사용자 ID") @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long courseId
    ) {
        courseUseCase.deleteCourse(userId, courseId);
        return ResponseEntity.ok(ApiResponse.success("강의가 삭제됐습니다.", null));
    }

    @Operation(summary = "강의 오픈", description = "DRAFT → OPEN 상태 전이")
    @PatchMapping("/{courseId}/open")
    public ResponseEntity<ApiResponse<CourseResponse>> openCourse(
            @Parameter(description = "사용자 ID") @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long courseId
    ) {
        CourseResponse response = courseUseCase.openCourse(userId, courseId);
        return ResponseEntity.ok(ApiResponse.success("강의 모집이 시작됐습니다.", response));
    }

    @Operation(summary = "강의 목록 조회", description = "상태 필터 및 페이지네이션 지원")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CourseResponse>>> getCourses(
            @Parameter(description = "강의 상태 필터 (DRAFT, OPEN, CLOSED)")
            @RequestParam(required = false) Course.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CourseResponse> response = courseUseCase.getCourses(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "강의 상세 조회", description = "현재 신청 인원 및 대기자 수 포함")
    @GetMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseDetailResponse>> getCourseDetail(
            @PathVariable Long courseId
    ) {
        CourseDetailResponse response = courseUseCase.getCourseDetail(courseId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "강의별 수강생 목록 조회", description = "CREATOR만 조회 가능, 페이지네이션 지원")
    @GetMapping("/{courseId}/students")
    public ResponseEntity<ApiResponse<Page<EnrolledStudentResponse>>> getEnrolledStudents(
            @Parameter(description = "사용자 ID") @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<EnrolledStudentResponse> response = courseUseCase.getEnrolledStudents(userId, courseId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}