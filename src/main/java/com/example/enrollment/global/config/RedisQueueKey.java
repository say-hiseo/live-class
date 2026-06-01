package com.example.enrollment.global.config;

public class RedisQueueKey {

    public static String enrollmentQueue(Long courseId) {
        return "queue:enrollment:" + courseId;
    }

    public static String enrollmentResult(String requestId) {
        return "result:enrollment:" + requestId;
    }

    public static String activeCourseQueues() {
        return "queue:enrollment:active-courses";
    }

    private RedisQueueKey() {}
}