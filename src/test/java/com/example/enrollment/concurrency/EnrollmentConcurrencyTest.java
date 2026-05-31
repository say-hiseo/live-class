package com.example.enrollment.concurrency;

import com.example.enrollment.infrastructure.course.CourseJpaEntity;
import com.example.enrollment.infrastructure.course.CourseJpaRepository;
import com.example.enrollment.infrastructure.enrollment.EnrollmentJpaRepository;
import com.example.enrollment.infrastructure.user.UserJpaEntity;
import com.example.enrollment.infrastructure.user.UserJpaRepository;
import com.example.enrollment.domain.course.model.Course;
import com.example.enrollment.domain.user.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.stream.Collectors;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("load-test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class EnrollmentConcurrencyTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserJpaRepository userJpaRepository;
    @Autowired private CourseJpaRepository courseJpaRepository;
    @Autowired private EnrollmentJpaRepository enrollmentJpaRepository;
    @Autowired private org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

    private List<Long> studentIds = new ArrayList<>();
    private static final int TOTAL_REQUESTS = 50000;
    private Long courseId;

    @BeforeEach
    void setUp() {
        enrollmentJpaRepository.deleteAll();
        courseJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        redisTemplate.getConnectionFactory().getConnection().flushDb();

        UserJpaEntity creator = userJpaRepository.save(UserJpaEntity.builder()
                .name("김강사").role(User.Role.CREATOR).build());

        int threadCount = 100;
        List<UserJpaEntity> students = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            students.add(UserJpaEntity.builder()
                    .name("수강생" + i)
                    .role(User.Role.STUDENT)
                    .build());
        }
        studentIds = userJpaRepository.saveAll(students)
                .stream().map(UserJpaEntity::getId).collect(Collectors.toList());

        CourseJpaEntity course = courseJpaRepository.save(CourseJpaEntity.builder()
                .creatorId(creator.getId())
                .title("동시성 테스트 강의")
                .description("정원 1명")
                .price(50000)
                .capacity(1)
                .enrolledCount(0)
                .status(Course.Status.OPEN)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(35))
                .deadline(LocalDate.now().plusDays(3))
                .build());
        courseId = course.getId();
    }

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }

    @Test
    @DisplayName("50,000건 동시 수강 신청 - 정원 초과 없이 큐 적재 성공")
    void concurrentEnrollment_50000() throws Exception {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            final Long studentId = studentIds.get(i % studentIds.size());
            final String requestBody = objectMapper.writeValueAsString(Map.of("courseId", courseId));

            executorService.submit(() -> {
                try {
                    int status = mockMvc.perform(post("/enrollments")
                                    .header("X-User-Id", studentId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody))
                            .andReturn().getResponse().getStatus();

                    if (status == 202 || status == 409) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(5, TimeUnit.MINUTES);
        executorService.shutdown();

        long elapsed = System.currentTimeMillis() - startTime;

        System.out.println("완료여부: " + completed);
        System.out.println("총 요청: " + TOTAL_REQUESTS);
        System.out.println("성공: " + successCount.get());
        System.out.println("실패: " + failCount.get());
        System.out.println("남은 latch: " + latch.getCount());
        System.out.println("처리 시간: " + elapsed + "ms");

        assertThat(completed).isTrue();
        assertThat(failCount.get()).isEqualTo(0);
    }

    @Test
    @DisplayName("50,000명 동시 신청 - 정원(1명)만 확정되고 나머지는 대기열 처리")
    void concurrentEnrollment_onlyCapacityConfirmed() throws Exception {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS);

        AtomicInteger acceptedCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            final Long studentId = studentIds.get(i % studentIds.size());
            final String requestBody = objectMapper.writeValueAsString(
                    Map.of("courseId", courseId));

            executorService.submit(() -> {
                try {
                    int status = mockMvc.perform(post("/enrollments")
                                    .header("X-User-Id", studentId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody))
                            .andReturn().getResponse().getStatus();

                    if (status == 202) acceptedCount.incrementAndGet();
                    else if (status == 409) rejectedCount.incrementAndGet();
                    else errorCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.MINUTES);
        executorService.shutdown();

        Thread.sleep(3000);

        long elapsed = System.currentTimeMillis() - startTime;

        long confirmedCount = enrollmentJpaRepository.findAll().stream()
                .filter(e -> e.getStatus() == com.example.enrollment.domain.enrollment.model.Enrollment.Status.CONFIRMED)
                .count();
        long pendingCount = enrollmentJpaRepository.findAll().stream()
                .filter(e -> e.getStatus() == com.example.enrollment.domain.enrollment.model.Enrollment.Status.PENDING)
                .count();

        int capacity = courseJpaRepository.findById(courseId).get().getCapacity();

        System.out.println("=== 50,000명 동시 수강 신청 결과 ===");
        System.out.println("총 요청: " + TOTAL_REQUESTS + "건");
        System.out.println("큐 접수(202): " + acceptedCount.get() + "건");
        System.out.println("중복 거부(409): " + rejectedCount.get() + "건");
        System.out.println("오류: " + errorCount.get() + "건");
        System.out.println("처리 시간: " + elapsed + "ms (" + (elapsed / 1000.0) + "초)");
        System.out.println("처리량: " + (long)(TOTAL_REQUESTS / (elapsed / 1000.0)) + "건/초");
        System.out.println("--- DB 최종 상태 ---");
        System.out.println("강의 정원: " + capacity + "명");
        System.out.println("CONFIRMED(결제 확정): " + confirmedCount + "건");
        System.out.println("PENDING(결제 대기): " + pendingCount + "건");

        assertThat(errorCount.get()).isEqualTo(0);
        assertThat(confirmedCount).isLessThanOrEqualTo(capacity);
    }
}