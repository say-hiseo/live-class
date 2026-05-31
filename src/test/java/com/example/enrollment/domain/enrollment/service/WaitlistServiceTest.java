package com.example.enrollment.domain.enrollment.service;

import com.example.enrollment.domain.course.model.Course;
import com.example.enrollment.domain.course.port.out.CoursePort;
import com.example.enrollment.domain.enrollment.dto.WaitlistResponse;
import com.example.enrollment.domain.enrollment.model.Enrollment;
import com.example.enrollment.domain.enrollment.model.Waitlist;
import com.example.enrollment.domain.enrollment.port.out.EnrollmentPort;
import com.example.enrollment.domain.enrollment.port.out.WaitlistPort;
import com.example.enrollment.domain.user.model.User;
import com.example.enrollment.domain.user.port.out.UserPort;
import com.example.enrollment.global.exception.RestApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WaitlistServiceTest {

    @InjectMocks
    private WaitlistService waitlistService;

    @Mock private WaitlistPort waitlistPort;
    @Mock private CoursePort coursePort;
    @Mock private UserPort userPort;
    @Mock private EnrollmentPort enrollmentPort;

    private User student;
    private User creator;
    private Course fullCourse;
    private Course openCourse;
    private Waitlist waitlist;

    @BeforeEach
    void setUp() {
        student = User.builder()
                .id(1L).name("박수강").role(User.Role.STUDENT).build();

        creator = User.builder()
                .id(2L).name("김강사").role(User.Role.CREATOR).build();

        fullCourse = Course.builder()
                .id(1L).creatorId(2L).title("인기 강의")
                .description("설명").price(50000).capacity(1).enrolledCount(1)
                .status(Course.Status.OPEN)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(35))
                .deadline(LocalDate.now().plusDays(3))
                .build();

        openCourse = Course.builder()
                .id(2L).creatorId(2L).title("일반 강의")
                .description("설명").price(50000).capacity(30).enrolledCount(0)
                .status(Course.Status.OPEN)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(35))
                .deadline(LocalDate.now().plusDays(3))
                .build();

        waitlist = Waitlist.builder()
                .id(1L).courseId(1L).userId(1L)
                .waitOrder(1).status(Waitlist.Status.WAITING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("대기열 등록 성공 - 정원이 꽉 찬 OPEN 강의에 등록 가능")
    void registerWaitlist_success() {
        given(userPort.findById(1L)).willReturn(Optional.of(student));
        given(coursePort.findById(1L)).willReturn(Optional.of(fullCourse));
        given(enrollmentPort.findActiveByCourseIdAndUserId(1L, 1L)).willReturn(Optional.empty());
        given(waitlistPort.findWaitingByCourseIdAndUserId(1L, 1L)).willReturn(Optional.empty());
        given(waitlistPort.countWaitingByCourseId(1L)).willReturn(0);
        given(waitlistPort.save(any())).willReturn(waitlist);

        WaitlistResponse response = waitlistService.registerWaitlist(1L, 1L);

        assertThat(response.getStatus()).isEqualTo(Waitlist.Status.WAITING);
        assertThat(response.getWaitOrder()).isEqualTo(1);
        verify(waitlistPort).save(any());
    }

    @Test
    @DisplayName("대기열 등록 실패 - 정원이 남은 강의는 대기열 등록 불가")
    void registerWaitlist_fail_courseNotFull() {
        given(userPort.findById(1L)).willReturn(Optional.of(student));
        given(coursePort.findById(2L)).willReturn(Optional.of(openCourse));

        assertThatThrownBy(() -> waitlistService.registerWaitlist(1L, 2L))
                .isInstanceOf(RestApiException.class);
    }

    @Test
    @DisplayName("대기열 등록 실패 - 이미 수강 신청한 강의는 대기열 등록 불가")
    void registerWaitlist_fail_alreadyEnrolled() {
        Enrollment activeEnrollment = Enrollment.builder()
                .id(1L).courseId(1L).userId(1L)
                .status(Enrollment.Status.CONFIRMED)
                .build();

        given(userPort.findById(1L)).willReturn(Optional.of(student));
        given(coursePort.findById(1L)).willReturn(Optional.of(fullCourse));
        given(enrollmentPort.findActiveByCourseIdAndUserId(1L, 1L))
                .willReturn(Optional.of(activeEnrollment));

        assertThatThrownBy(() -> waitlistService.registerWaitlist(1L, 1L))
                .isInstanceOf(RestApiException.class);
    }

    @Test
    @DisplayName("대기열 등록 실패 - 이미 대기열에 등록된 강의는 중복 등록 불가")
    void registerWaitlist_fail_alreadyWaitlisted() {
        given(userPort.findById(1L)).willReturn(Optional.of(student));
        given(coursePort.findById(1L)).willReturn(Optional.of(fullCourse));
        given(enrollmentPort.findActiveByCourseIdAndUserId(1L, 1L)).willReturn(Optional.empty());
        given(waitlistPort.findWaitingByCourseIdAndUserId(1L, 1L))
                .willReturn(Optional.of(waitlist));

        assertThatThrownBy(() -> waitlistService.registerWaitlist(1L, 1L))
                .isInstanceOf(RestApiException.class);
    }

    @Test
    @DisplayName("대기열 등록 실패 - CREATOR는 대기열 등록 불가")
    void registerWaitlist_fail_notStudent() {
        given(userPort.findById(2L)).willReturn(Optional.of(creator));

        assertThatThrownBy(() -> waitlistService.registerWaitlist(2L, 1L))
                .isInstanceOf(RestApiException.class);
    }

    @Test
    @DisplayName("대기열 취소 성공")
    void cancelWaitlist_success() {
        given(userPort.findById(1L)).willReturn(Optional.of(student));
        given(waitlistPort.findById(1L)).willReturn(Optional.of(waitlist));
        given(waitlistPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        WaitlistResponse response = waitlistService.cancelWaitlist(1L, 1L);

        assertThat(response.getStatus()).isEqualTo(Waitlist.Status.CANCELLED);
        verify(waitlistPort).save(any());
    }

    @Test
    @DisplayName("대기열 취소 실패 - 본인 대기열이 아니면 취소 불가")
    void cancelWaitlist_fail_notOwner() {
        User otherStudent = User.builder()
                .id(3L).name("다른수강생").role(User.Role.STUDENT).build();

        given(userPort.findById(3L)).willReturn(Optional.of(otherStudent));
        given(waitlistPort.findById(1L)).willReturn(Optional.of(waitlist));

        assertThatThrownBy(() -> waitlistService.cancelWaitlist(3L, 1L))
                .isInstanceOf(RestApiException.class);
    }

    @Test
    @DisplayName("내 대기열 조회 성공")
    void getMyWaitlist_success() {
        given(userPort.findById(1L)).willReturn(Optional.of(student));
        given(coursePort.findById(1L)).willReturn(Optional.of(fullCourse));
        given(waitlistPort.findWaitingByCourseIdAndUserId(1L, 1L))
                .willReturn(Optional.of(waitlist));

        WaitlistResponse response = waitlistService.getMyWaitlist(1L, 1L);

        assertThat(response.getCourseId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(Waitlist.Status.WAITING);
    }
}