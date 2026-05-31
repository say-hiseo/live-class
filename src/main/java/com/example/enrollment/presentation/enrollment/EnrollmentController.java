package com.example.enrollment.presentation.enrollment;

import com.example.enrollment.domain.enrollment.dto.*;
import com.example.enrollment.domain.enrollment.port.in.EnrollmentUseCase;
import com.example.enrollment.domain.enrollment.port.in.WaitlistUseCase;
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

@Tag(name = "Enrollment", description = "수강 신청 관리 API")
@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentUseCase enrollmentUseCase;
    private final WaitlistUseCase waitlistUseCase;

    @Operation(summary = "수강 신청 요청",
            description = "Redis 큐에 신청을 적재합니다. requestId로 결과를 조회하세요.")
    @PostMapping
    public ResponseEntity<ApiResponse<String>> requestEnrollment(
            @Parameter(description = "사용자 ID") @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody EnrollmentCreateRequest request
    ) {
        String requestId = enrollmentUseCase.requestEnrollment(userId, request.getCourseId());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("신청이 접수됐습니다. requestId로 결과를 확인해주세요.", requestId));
    }

    @Operation(summary = "수강 신청 결과 조회",
            description = "requestId로 신청 처리 결과를 조회합니다. (PENDING/SUCCESS/FAIL)")
    @GetMapping("/result/{requestId}")
    public ResponseEntity<ApiResponse<EnrollmentResultResponse>> getEnrollmentResult(
            @PathVariable String requestId
    ) {
        EnrollmentResultResponse response = enrollmentUseCase.getEnrollmentResult(requestId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "결제 확정", description = "PENDING → CONFIRMED 상태 전이")
    @PatchMapping("/{enrollmentId}/confirm")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> confirmEnrollment(
            @Parameter(description = "사용자 ID") @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long enrollmentId
    ) {
        EnrollmentResponse response = enrollmentUseCase.confirmEnrollment(userId, enrollmentId);
        return ResponseEntity.ok(ApiResponse.success("결제가 확정됐습니다.", response));
    }

    @Operation(summary = "수강 취소",
            description = "PENDING → CANCELLED 또는 CONFIRMED → CANCELLED (7일 이내)")
    @PatchMapping("/{enrollmentId}/cancel")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> cancelEnrollment(
            @Parameter(description = "사용자 ID") @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long enrollmentId,
            @RequestBody(required = false) EnrollmentCancelRequest request
    ) {
        if (request == null) request = new EnrollmentCancelRequest();
        EnrollmentResponse response = enrollmentUseCase.cancelEnrollment(userId, enrollmentId, request);
        return ResponseEntity.ok(ApiResponse.success("수강이 취소됐습니다.", response));
    }

    @Operation(summary = "내 수강 신청 목록 조회", description = "페이지네이션 지원")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<EnrollmentResponse>>> getMyEnrollments(
            @Parameter(description = "사용자 ID") @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<EnrollmentResponse> response = enrollmentUseCase.getMyEnrollments(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "수강 신청 단건 조회")
    @GetMapping("/{enrollmentId}")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> getEnrollment(
            @Parameter(description = "사용자 ID") @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long enrollmentId
    ) {
        EnrollmentResponse response = enrollmentUseCase.getEnrollment(userId, enrollmentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "대기열 등록", description = "정원이 꽉 찬 OPEN 강의에만 등록 가능합니다.")
    @PostMapping("/waitlist")
    public ResponseEntity<ApiResponse<WaitlistResponse>> registerWaitlist(
            @Parameter(description = "사용자 ID") @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody WaitlistCreateRequest request
    ) {
        WaitlistResponse response = waitlistUseCase.registerWaitlist(userId, request.getCourseId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("대기열에 등록됐습니다.", response));
    }

    @Operation(summary = "대기열 취소")
    @PatchMapping("/waitlist/{waitlistId}/cancel")
    public ResponseEntity<ApiResponse<WaitlistResponse>> cancelWaitlist(
            @Parameter(description = "사용자 ID") @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long waitlistId
    ) {
        WaitlistResponse response = waitlistUseCase.cancelWaitlist(userId, waitlistId);
        return ResponseEntity.ok(ApiResponse.success("대기열이 취소됐습니다.", response));
    }

    @Operation(summary = "내 대기열 조회")
    @GetMapping("/waitlist/my")
    public ResponseEntity<ApiResponse<WaitlistResponse>> getMyWaitlist(
            @Parameter(description = "사용자 ID") @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long courseId
    ) {
        WaitlistResponse response = waitlistUseCase.getMyWaitlist(userId, courseId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}