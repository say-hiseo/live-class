package com.example.enrollment.global.config;

public class RedisQueueKey {

    public static String enrollmentQueue(Long courseId) {
        return "queue:enrollment:" + courseId;
    }

    public static String enrollmentResult(String requestId) {
        return "result:enrollment:" + requestId;
    }

    // 활성 큐 목록 관리용 Set 키
    public static String activeCourseQueues() {
        return "queue:enrollment:active-courses";
    }

    private RedisQueueKey() {}
}