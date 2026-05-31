package com.example.enrollment.domain.enrollment.service;

import com.example.enrollment.domain.course.model.Course;
import com.example.enrollment.domain.course.port.out.CoursePort;
import com.example.enrollment.domain.enrollment.dto.EnrollmentCancelRequest;
import com.example.enrollment.domain.enrollment.dto.EnrollmentResponse;
import com.example.enrollment.domain.enrollment.model.Enrollment;
import com.example.enrollment.domain.enrollment.port.out.EnrollmentPort;
import com.example.enrollment.domain.enrollment.port.out.WaitlistPort;
import com.example.enrollment.domain.user.model.User;
import com.example.enrollment.domain.user.port.out.UserPort;
import com.example.enrollment.global.exception.RestApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @InjectMocks
    private EnrollmentService enrollmentService;

    @Mock private EnrollmentPort enrollmentPort;
    @Mock private CoursePort coursePort;
    @Mock private UserPort userPort;
    @Mock private WaitlistPort waitlistPort;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ObjectMapper objectMapper;
    @Mock private ValueOperations<String, String> valueOperations;

    private User student;
    private User creator;
    private Course openCourse;
    private Course fullCourse;
    private Enrollment pendingEnrollment;
    private Enrollment confirmedEnrollment;

    @BeforeEach
    void setUp() {
        student = User.builder()
                .id(1L).name("박수강").role(User.Role.STUDENT).build();

        creator = User.builder()
                .id(2L).name("김강사").role(User.Role.CREATOR).build();

        openCourse = Course.builder()
                .id(1L).creatorId(2L).title("Spring Boot 입문")
                .description("설명").price(50000).capacity(30).enrolledCount(1)
                .status(Course.Status.OPEN)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(35))
                .deadline(LocalDate.now().plusDays(3))
                .build();

        fullCourse = Course.builder()
                .id(2L).creatorId(2L).title("인기 강의")
                .description("설명").price(50000).capacity(1).enrolledCount(1)
                .status(Course.Status.OPEN)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(35))
                .deadline(LocalDate.now().plusDays(3))
                .build();

        pendingEnrollment = Enrollment.builder()
                .id(1L).courseId(1L).userId(1L)
                .status(Enrollment.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        confirmedEnrollment = Enrollment.builder()
                .id(2L).courseId(1L).userId(1L)
                .status(Enrollment.Status.CONFIRMED)
                .confirmedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("결제 확정 성공 - PENDING → CONFIRMED 상태 전이")
    void confirmEnrollment_success() {
        given(userPort.findById(1L)).willReturn(Optional.of(student));
        given(enrollmentPort.findById(1L)).willReturn(Optional.of(pendingEnrollment));
        given(coursePort.findByIdWithLock(1L)).willReturn(Optional.of(openCourse));
        given(enrollmentPort.save(any())).willReturn(pendingEnrollment);
        given(coursePort.save(any())).willReturn(openCourse);

        EnrollmentResponse response = enrollmentService.confirmEnrollment(1L, 1L);

        assertThat(response.getStatus()).isEqualTo(Enrollment.Status.CONFIRMED);
        verify(enrollmentPort).save(any());
        verify(coursePort).save(any());
    }

    @Test
    @DisplayName("결제 확정 실패 - 정원 초과 시 결제 거부 및 PENDING 취소")
    void confirmEnrollment_fail_courseFull() {
        Enrollment pendingForFullCourse = Enrollment.builder()
                .id(3L).courseId(2L).userId(1L)
                .status(Enrollment.Status.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        given(userPort.findById(1L)).willReturn(Optional.of(student));
        given(enrollmentPort.findById(3L)).willReturn(Optional.of(pendingForFullCourse));
        given(coursePort.findByIdWithLock(2L)).willReturn(Optional.of(fullCourse));
        given(enrollmentPort.save(any())).willReturn(pendingForFullCourse);

        assertThatThrownBy(() -> enrollmentService.confirmEnrollment(1L, 3L))
                .isInstanceOf(RestApiException.class);
    }

    @Test
    @DisplayName("수강 취소 성공 - PENDING → CANCELLED")
    void cancelEnrollment_pending_success() {
        given(userPort.findById(1L)).willReturn(Optional.of(student));
        given(enrollmentPort.findById(1L)).willReturn(Optional.of(pendingEnrollment));
        given(enrollmentPort.save(any())).willReturn(pendingEnrollment);

        EnrollmentCancelRequest request = new EnrollmentCancelRequest();

        EnrollmentResponse response = enrollmentService.cancelEnrollment(1L, 1L, request);

        assertThat(response.getStatus()).isEqualTo(Enrollment.Status.CANCELLED);
    }

    @Test
    @DisplayName("수강 취소 성공 - CONFIRMED → CANCELLED (7일 이내)")
    void cancelEnrollment_confirmed_success() {
        Enrollment freshConfirmed = Enrollment.builder()
                .id(2L).courseId(1L).userId(1L)
                .status(Enrollment.Status.CONFIRMED)
                .confirmedAt(LocalDateTime.now().minusDays(1)) // 1일 전 확정
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        given(userPort.findById(1L)).willReturn(Optional.of(student));
        given(enrollmentPort.findById(2L)).willReturn(Optional.of(freshConfirmed));
        given(coursePort.findByIdWithLock(1L)).willReturn(Optional.of(openCourse));
        given(enrollmentPort.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(coursePort.save(any())).willReturn(openCourse);
        given(waitlistPort.findFirstWaitingByCourseId(1L)).willReturn(Optional.empty());

        EnrollmentCancelRequest request = new EnrollmentCancelRequest();

        EnrollmentResponse response = enrollmentService.cancelEnrollment(1L, 2L, request);

        assertThat(response.getStatus()).isEqualTo(Enrollment.Status.CANCELLED);
        verify(coursePort).save(any());
    }

    @Test
    @DisplayName("수강 취소 실패 - 7일 초과 시 취소 불가")
    void cancelEnrollment_fail_expiredCancelPeriod() {
        Enrollment expiredEnrollment = Enrollment.builder()
                .id(3L).courseId(1L).userId(1L)
                .status(Enrollment.Status.CONFIRMED)
                .confirmedAt(LocalDateTime.now().minusDays(8)) // 8일 전 확정
                .createdAt(LocalDateTime.now().minusDays(8))
                .build();

        given(userPort.findById(1L)).willReturn(Optional.of(student));
        given(enrollmentPort.findById(3L)).willReturn(Optional.of(expiredEnrollment));

        EnrollmentCancelRequest request = new EnrollmentCancelRequest();

        assertThatThrownBy(() -> enrollmentService.cancelEnrollment(1L, 3L, request))
                .isInstanceOf(RestApiException.class);
    }

    @Test
    @DisplayName("수강 취소 실패 - 본인 신청이 아니면 취소 불가")
    void cancelEnrollment_fail_notOwner() {
        User otherStudent = User.builder()
                .id(3L).name("다른수강생").role(User.Role.STUDENT).build();

        given(userPort.findById(3L)).willReturn(Optional.of(otherStudent));
        given(enrollmentPort.findById(1L)).willReturn(Optional.of(pendingEnrollment));

        EnrollmentCancelRequest request = new EnrollmentCancelRequest();

        assertThatThrownBy(() -> enrollmentService.cancelEnrollment(3L, 1L, request))
                .isInstanceOf(RestApiException.class);
    }

    @Test
    @DisplayName("CREATOR는 수강 취소를 할 수 없다")
    void cancelEnrollment_fail_notStudent() {
        given(userPort.findById(2L)).willReturn(Optional.of(creator));

        EnrollmentCancelRequest request = new EnrollmentCancelRequest();

        assertThatThrownBy(() -> enrollmentService.cancelEnrollment(2L, 1L, request))
                .isInstanceOf(RestApiException.class);
    }
}