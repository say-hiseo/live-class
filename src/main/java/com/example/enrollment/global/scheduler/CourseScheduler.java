package com.example.enrollment.global.scheduler;

import com.example.enrollment.domain.course.model.Course;
import com.example.enrollment.domain.course.port.out.CoursePort;
import com.example.enrollment.domain.enrollment.model.Waitlist;
import com.example.enrollment.domain.enrollment.port.out.WaitlistPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseScheduler {

    private final CoursePort coursePort;
    private final WaitlistPort waitlistPort;

    // 매일 자정에 deadline 초과 강의 CLOSED 처리
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void closeExpiredCourses() {
        List<Course> expiredCourses = coursePort.findOpenCoursesWithExpiredDeadline();

        for (Course course : expiredCourses) {
            try {
                course.close();
                coursePort.save(course);

                cancelAllWaitlists(course.getId());

                log.info("강의 자동 마감 완료 - courseId: {}, title: {}",
                        course.getId(), course.getTitle());
            } catch (Exception e) {
                log.error("강의 자동 마감 실패 - courseId: {}, error: {}",
                        course.getId(), e.getMessage());
            }
        }
    }

    private void cancelAllWaitlists(Long courseId) {
        List<Waitlist> waitlists = waitlistPort.findAllWaitingByCourseId(courseId);
        for (Waitlist waitlist : waitlists) {
            waitlist.cancel();
            waitlistPort.save(waitlist);
        }
        if (!waitlists.isEmpty()) {
            log.info("대기자 자동 취소 완료 - courseId: {}, count: {}",
                    courseId, waitlists.size());
        }
    }
}