package com.example.enrollment.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 BAD_REQUEST
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "유효하지 않은 상태 전이입니다."),
    ENROLLMENT_EXPIRED(HttpStatus.BAD_REQUEST, "결제 기한이 만료된 신청입니다."),
    CANCEL_PERIOD_EXPIRED(HttpStatus.BAD_REQUEST, "취소 가능 기간이 초과되었습니다."),
    PAYMENT_REJECTED(HttpStatus.BAD_REQUEST, "정원이 마감되어 결제가 취소됐습니다. 대기열에 등록해주세요."),

    // 403 FORBIDDEN
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_CREATOR(HttpStatus.FORBIDDEN, "강사만 이용할 수 있는 기능입니다."),
    NOT_STUDENT(HttpStatus.FORBIDDEN, "수강생만 이용할 수 있는 기능입니다."),
    NOT_COURSE_OWNER(HttpStatus.FORBIDDEN, "본인의 강의만 관리할 수 있습니다."),

    // 404 NOT_FOUND
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "강의를 찾을 수 없습니다."),
    ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "수강 신청을 찾을 수 없습니다."),
    WAITLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "대기열을 찾을 수 없습니다."),

    // 409 CONFLICT
    CONFLICT(HttpStatus.CONFLICT, "리소스 상태와 충돌이 발생했습니다."),
    ALREADY_ENROLLED(HttpStatus.CONFLICT, "이미 신청한 강의입니다."),
    ALREADY_WAITLISTED(HttpStatus.CONFLICT, "이미 대기열에 등록된 강의입니다."),
    COURSE_FULL(HttpStatus.CONFLICT, "정원이 초과된 강의입니다. 대기열에 등록해주세요."),
    COURSE_NOT_OPEN(HttpStatus.CONFLICT, "모집 중인 강의만 신청할 수 있습니다."),
    COURSE_NOT_DELETABLE(HttpStatus.CONFLICT, "초안 상태의 강의만 삭제할 수 있습니다."),

    // 500 INTERNAL_SERVER_ERROR
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "종료일은 시작일 이후여야 합니다."),
    INVALID_DEADLINE(HttpStatus.BAD_REQUEST, "모집 마감일은 시작일 이후, 종료일 이전이어야 합니다.");

    private final HttpStatus httpStatus;
    private final String message;
}