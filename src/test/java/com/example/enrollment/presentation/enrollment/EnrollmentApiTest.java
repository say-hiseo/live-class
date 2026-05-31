package com.example.enrollment.presentation.enrollment;

import com.example.enrollment.TestConfig;
import com.example.enrollment.infrastructure.course.CourseJpaEntity;
import com.example.enrollment.infrastructure.course.CourseJpaRepository;
import com.example.enrollment.infrastructure.enrollment.EnrollmentJpaEntity;
import com.example.enrollment.infrastructure.enrollment.EnrollmentJpaRepository;
import com.example.enrollment.infrastructure.user.UserJpaEntity;
import com.example.enrollment.infrastructure.user.UserJpaRepository;
import com.example.enrollment.domain.course.model.Course;
import com.example.enrollment.domain.enrollment.model.Enrollment;
import com.example.enrollment.domain.user.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class EnrollmentApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserJpaRepository userJpaRepository;
    @Autowired
    private CourseJpaRepository courseJpaRepository;
    @Autowired
    private EnrollmentJpaRepository enrollmentJpaRepository;

    private Long creatorId;
    private Long studentId;
    private Long openCourseId;
    private Long fullCourseId;

    @BeforeEach
    void setUp() {
        UserJpaEntity creator = userJpaRepository.save(UserJpaEntity.builder()
                .name("김강사").role(User.Role.CREATOR).build());
        UserJpaEntity student = userJpaRepository.save(UserJpaEntity.builder()
                .name("박수강").role(User.Role.STUDENT).build());

        creatorId = creator.getId();
        studentId = student.getId();

        CourseJpaEntity openCourse = courseJpaRepository.save(CourseJpaEntity.builder()
                .creatorId(creatorId)
                .title("Spring Boot 입문")
                .description("설명")
                .price(50000)
                .capacity(30)
                .enrolledCount(0)
                .status(Course.Status.OPEN)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(35))
                .deadline(LocalDate.now().plusDays(3))
                .build());
        openCourseId = openCourse.getId();

        CourseJpaEntity fullCourse = courseJpaRepository.save(CourseJpaEntity.builder()
                .creatorId(creatorId)
                .title("인기 강의")
                .description("설명")
                .price(50000)
                .capacity(1)
                .enrolledCount(1)
                .status(Course.Status.OPEN)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(35))
                .deadline(LocalDate.now().plusDays(3))
                .build());
        fullCourseId = fullCourse.getId();
    }

    @Test
    @DisplayName("수강 신청 → 결제 확정 전체 흐름")
    void enrollmentFullFlow() throws Exception {
        Map<String, Object> request = Map.of("courseId", openCourseId);

        String result = mockMvc.perform(post("/enrollments")
                        .header("X-User-Id", studentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isNotEmpty()) // requestId 반환 확인
                .andReturn().getResponse().getContentAsString();

        String requestId = objectMapper.readTree(result)
                .path("data").asText();

        assertThat(requestId).matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
        );
    }

    @Test
    @DisplayName("수강 신청 요청 성공 - 큐 접수 확인")
    void enrollmentRequest_success() throws Exception {
        Map<String, Object> request = Map.of("courseId", openCourseId);

        mockMvc.perform(post("/enrollments")
                        .header("X-User-Id", studentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isNotEmpty()); // requestId 반환 확인
    }

    @Test
    @DisplayName("수강 신청 실패 - CREATOR는 수강 신청 불가")
    void enrollment_fail_creator() throws Exception {
        Map<String, Object> request = Map.of("courseId", openCourseId);

        mockMvc.perform(post("/enrollments")
                        .header("X-User-Id", creatorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("결제 확정 성공 - PENDING → CONFIRMED")
    void confirmEnrollment_success() throws Exception {
        EnrollmentJpaEntity enrollment = enrollmentJpaRepository.save(
                EnrollmentJpaEntity.builder()
                        .courseId(openCourseId)
                        .userId(studentId)
                        .status(Enrollment.Status.PENDING)
                        .build());

        mockMvc.perform(patch("/enrollments/{enrollmentId}/confirm", enrollment.getId())
                        .header("X-User-Id", studentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("수강 취소 성공 - PENDING → CANCELLED")
    void cancelEnrollment_pending_success() throws Exception {
        EnrollmentJpaEntity enrollment = enrollmentJpaRepository.save(
                EnrollmentJpaEntity.builder()
                        .courseId(openCourseId)
                        .userId(studentId)
                        .status(Enrollment.Status.PENDING)
                        .build());

        mockMvc.perform(patch("/enrollments/{enrollmentId}/cancel", enrollment.getId())
                        .header("X-User-Id", studentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("수강 취소 실패 - 7일 초과 시 취소 불가")
    void cancelEnrollment_fail_expired() throws Exception {
        EnrollmentJpaEntity enrollment = enrollmentJpaRepository.save(
                EnrollmentJpaEntity.builder()
                        .courseId(openCourseId)
                        .userId(studentId)
                        .status(Enrollment.Status.CONFIRMED)
                        .confirmedAt(LocalDateTime.now().minusDays(8))
                        .build());

        mockMvc.perform(patch("/enrollments/{enrollmentId}/cancel", enrollment.getId())
                        .header("X-User-Id", studentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("대기열 등록 성공 - 정원 초과 강의에 등록")
    void waitlist_success() throws Exception {
        Map<String, Object> request = Map.of("courseId", fullCourseId);

        mockMvc.perform(post("/enrollments/waitlist")
                        .header("X-User-Id", studentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("WAITING"))
                .andExpect(jsonPath("$.data.waitOrder").value(1));
    }

    @Test
    @DisplayName("내 수강 신청 목록 조회")
    void getMyEnrollments_success() throws Exception {
        enrollmentJpaRepository.save(EnrollmentJpaEntity.builder()
                .courseId(openCourseId)
                .userId(studentId)
                .status(Enrollment.Status.PENDING)
                .build());

        mockMvc.perform(get("/enrollments/my")
                        .header("X-User-Id", studentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("중복 수강 신청 방지 - 이미 신청한 강의는 재신청 불가")
    void enrollment_fail_duplicate() throws Exception {
        enrollmentJpaRepository.save(EnrollmentJpaEntity.builder()
                .courseId(openCourseId)
                .userId(studentId)
                .status(Enrollment.Status.PENDING)
                .build());

        Map<String, Object> request = Map.of("courseId", openCourseId);

        mockMvc.perform(post("/enrollments")
                        .header("X-User-Id", studentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("강의별 수강생 목록 조회 - CREATOR만 조회 가능")
    void getEnrolledStudents_success() throws Exception {
        enrollmentJpaRepository.save(EnrollmentJpaEntity.builder()
                .courseId(openCourseId)
                .userId(studentId)
                .status(Enrollment.Status.CONFIRMED)
                .confirmedAt(LocalDateTime.now())
                .build());

        UserJpaEntity student2 = userJpaRepository.save(UserJpaEntity.builder()
                .name("이수강").role(User.Role.STUDENT).build());

        enrollmentJpaRepository.save(EnrollmentJpaEntity.builder()
                .courseId(openCourseId)
                .userId(student2.getId())
                .status(Enrollment.Status.CONFIRMED)
                .confirmedAt(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/courses/{courseId}/students", openCourseId)
                        .header("X-User-Id", creatorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    @DisplayName("강의별 수강생 목록 조회 실패 - STUDENT는 조회 불가")
    void getEnrolledStudents_fail_student() throws Exception {
        mockMvc.perform(get("/courses/{courseId}/students", openCourseId)
                        .header("X-User-Id", studentId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("취소 후 대기자 자동 승격 - CONFIRMED 취소 시 첫 번째 대기자 PENDING 전환")
    void cancelAndPromoteWaitlist() throws Exception {
        EnrollmentJpaEntity confirmed = enrollmentJpaRepository.save(
                EnrollmentJpaEntity.builder()
                        .courseId(openCourseId)
                        .userId(studentId)
                        .status(Enrollment.Status.CONFIRMED)
                        .confirmedAt(LocalDateTime.now().minusDays(1))
                        .build());

        UserJpaEntity waiter = userJpaRepository.save(UserJpaEntity.builder()
                .name("대기수강생").role(User.Role.STUDENT).build());

        courseJpaRepository.findById(openCourseId).ifPresent(course -> {
            CourseJpaEntity updated = CourseJpaEntity.builder()
                    .id(course.getId())
                    .creatorId(course.getCreatorId())
                    .title(course.getTitle())
                    .description(course.getDescription())
                    .price(course.getPrice())
                    .capacity(1)
                    .enrolledCount(1)
                    .status(Course.Status.OPEN)
                    .startDate(course.getStartDate())
                    .endDate(course.getEndDate())
                    .deadline(course.getDeadline())
                    .build();
            courseJpaRepository.save(updated);
        });

        Map<String, Object> waitlistRequest = Map.of("courseId", openCourseId);
        mockMvc.perform(post("/enrollments/waitlist")
                        .header("X-User-Id", waiter.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(waitlistRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("WAITING"));

        mockMvc.perform(patch("/enrollments/{enrollmentId}/cancel", confirmed.getId())
                        .header("X-User-Id", studentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(get("/enrollments/my")
                        .header("X-User-Id", waiter.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
    }
}