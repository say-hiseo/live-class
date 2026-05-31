package com.example.enrollment.presentation.course;

import com.example.enrollment.TestConfig;
import com.example.enrollment.infrastructure.course.CourseJpaRepository;
import com.example.enrollment.infrastructure.user.UserJpaEntity;
import com.example.enrollment.infrastructure.user.UserJpaRepository;
import com.example.enrollment.domain.user.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CourseApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserJpaRepository userJpaRepository;
    @Autowired private CourseJpaRepository courseJpaRepository;

    private Long creatorId;
    private Long studentId;
    private Long courseId;

    @BeforeEach
    void setUp() {
        UserJpaEntity creator = userJpaRepository.save(UserJpaEntity.builder()
                .name("김강사").role(User.Role.CREATOR).build());
        UserJpaEntity student = userJpaRepository.save(UserJpaEntity.builder()
                .name("박수강").role(User.Role.STUDENT).build());

        creatorId = creator.getId();
        studentId = student.getId();
    }

    @Test
    @DisplayName("강의 등록 → 오픈 → 조회 전체 흐름")
    void courseFullFlow() throws Exception {
        Map<String, Object> createRequest = Map.of(
                "title", "Spring Boot 입문",
                "description", "설명",
                "price", 50000,
                "capacity", 30,
                "startDate", LocalDate.now().plusDays(10).toString(),
                "endDate", LocalDate.now().plusDays(40).toString(),
                "deadline", LocalDate.now().plusDays(7).toString()
        );

        String createResult = mockMvc.perform(post("/courses")
                        .header("X-User-Id", creatorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        Long courseId = objectMapper.readTree(createResult)
                .path("data").path("id").asLong();

        mockMvc.perform(patch("/courses/{courseId}/open", courseId)
                        .header("X-User-Id", creatorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        mockMvc.perform(get("/courses")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/courses/{courseId}", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enrolledCount").value(0))
                .andExpect(jsonPath("$.data.remainingCount").value(30));
    }

    @Test
    @DisplayName("강의 등록 실패 - STUDENT는 강의 등록 불가")
    void createCourse_fail_student() throws Exception {
        Map<String, Object> request = Map.of(
                "title", "Spring Boot 입문",
                "description", "설명",
                "price", 50000,
                "capacity", 30,
                "startDate", LocalDate.now().plusDays(10).toString(),
                "endDate", LocalDate.now().plusDays(40).toString(),
                "deadline", LocalDate.now().plusDays(7).toString()
        );

        mockMvc.perform(post("/courses")
                        .header("X-User-Id", studentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("강의 삭제 성공 - DRAFT 상태만 삭제 가능")
    void deleteCourse_success() throws Exception {
        Map<String, Object> request = Map.of(
                "title", "삭제할 강의",
                "description", "설명",
                "price", 50000,
                "capacity", 30,
                "startDate", LocalDate.now().plusDays(10).toString(),
                "endDate", LocalDate.now().plusDays(40).toString(),
                "deadline", LocalDate.now().plusDays(7).toString()
        );

        String result = mockMvc.perform(post("/courses")
                        .header("X-User-Id", creatorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long courseId = objectMapper.readTree(result)
                .path("data").path("id").asLong();

        mockMvc.perform(delete("/courses/{courseId}", courseId)
                        .header("X-User-Id", creatorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("강의 삭제 실패 - OPEN 상태는 삭제 불가")
    void deleteCourse_fail_openStatus() throws Exception {
        Map<String, Object> request = Map.of(
                "title", "삭제 불가 강의",
                "description", "설명",
                "price", 50000,
                "capacity", 30,
                "startDate", LocalDate.now().plusDays(10).toString(),
                "endDate", LocalDate.now().plusDays(40).toString(),
                "deadline", LocalDate.now().plusDays(7).toString()
        );

        String result = mockMvc.perform(post("/courses")
                        .header("X-User-Id", creatorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long courseId = objectMapper.readTree(result)
                .path("data").path("id").asLong();

        mockMvc.perform(patch("/courses/{courseId}/open", courseId)
                        .header("X-User-Id", creatorId))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/courses/{courseId}", courseId)
                        .header("X-User-Id", creatorId))
                .andExpect(status().isConflict());
    }
}