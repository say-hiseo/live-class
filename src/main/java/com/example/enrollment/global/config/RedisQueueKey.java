package com.example.enrollment.global.config;

public class RedisQueueKey {

    // 수강 신청 요청 큐 (course_id별)
    public static String enrollmentQueue(Long courseId) {
        return "queue:enrollment:" + courseId;
    }

    // 수강 신청 결과 저장 (enrollment_id별)
    public static String enrollmentResult(String requestId) {
        return "result:enrollment:" + requestId;
    }

    private RedisQueueKey() {}
}