package com.example.enrollment.domain.course.service;

import com.example.enrollment.domain.course.dto.CourseCreateRequest;
import com.example.enrollment.domain.course.dto.CourseResponse;
import com.example.enrollment.domain.course.model.Course;
import com.example.enrollment.domain.course.port.out.CoursePort;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CourseServiceTest {

    @InjectMocks
    private CourseService courseService;

    @Mock private CoursePort coursePort;
    @Mock private UserPort userPort;
    @Mock private EnrollmentPort enrollmentPort;
    @Mock private WaitlistPort waitlistPort;

    private User creator;
    private User student;
    private Course course;

    @BeforeEach
    void setUp() {
        creator = User.builder()
                .id(1L)
                .name("김강사")
                .role(User.Role.CREATOR)
                .build();

        student = User.builder()
                .id(2L)
                .name("박수강")
                .role(User.Role.STUDENT)
                .build();

        course = Course.builder()
                .id(1L)
                .creatorId(1L)
                .title("Spring Boot 입문")
                .description("설명")
                .price(50000)
                .capacity(30)
                .enrolledCount(0)
                .status(Course.Status.DRAFT)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(40))
                .deadline(LocalDate.now().plusDays(7))
                .build();
    }

    @Test
    @DisplayName("강의 등록 성공 - CREATOR는 강의를 등록할 수 있다")
    void createCourse_success() {
        CourseCreateRequest request = new CourseCreateRequest();
        setField(request, "title", "Spring Boot 입문");
        setField(request, "description", "설명");
        setField(request, "price", 50000);
        setField(request, "capacity", 30);
        setField(request, "startDate", LocalDate.now().plusDays(10));
        setField(request, "endDate", LocalDate.now().plusDays(40));
        setField(request, "deadline", LocalDate.now().plusDays(7));

        given(userPort.findById(1L)).willReturn(Optional.of(creator));
        given(coursePort.save(any(Course.class))).willReturn(course);

        CourseResponse response = courseService.createCourse(1L, request);

        assertThat(response.getTitle()).isEqualTo("Spring Boot 입문");
        assertThat(response.getStatus()).isEqualTo(Course.Status.DRAFT);
        verify(coursePort).save(any(Course.class));
    }

    @Test
    @DisplayName("강의 등록 실패 - STUDENT는 강의를 등록할 수 없다")
    void createCourse_fail_notCreator() {
        CourseCreateRequest request = new CourseCreateRequest();
        setField(request, "title", "Spring Boot 입문");
        setField(request, "description", "설명");
        setField(request, "price", 50000);
        setField(request, "capacity", 30);
        setField(request, "startDate", LocalDate.now().plusDays(10));
        setField(request, "endDate", LocalDate.now().plusDays(40));
        setField(request, "deadline", LocalDate.now().plusDays(7));

        given(userPort.findById(2L)).willReturn(Optional.of(student));

        assertThatThrownBy(() -> courseService.createCourse(2L, request))
                .isInstanceOf(RestApiException.class);
    }

    @Test
    @DisplayName("강의 오픈 성공 - DRAFT → OPEN 상태 전이")
    void openCourse_success() {
        Course openedCourse = Course.builder()
                .id(1L).creatorId(1L).title("Spring Boot 입문")
                .description("설명").price(50000).capacity(30).enrolledCount(0)
                .status(Course.Status.OPEN)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(40))
                .deadline(LocalDate.now().plusDays(7))
                .build();

        given(userPort.findById(1L)).willReturn(Optional.of(creator));
        given(coursePort.findById(1L)).willReturn(Optional.of(course));
        given(coursePort.save(any(Course.class))).willReturn(openedCourse);

        CourseResponse response = courseService.openCourse(1L, 1L);

        assertThat(response.getStatus()).isEqualTo(Course.Status.OPEN);
    }

    @Test
    @DisplayName("강의 오픈 실패 - 본인 강의가 아니면 오픈 불가")
    void openCourse_fail_notOwner() {
        User otherCreator = User.builder()
                .id(3L).name("다른강사").role(User.Role.CREATOR).build();

        given(userPort.findById(3L)).willReturn(Optional.of(otherCreator));
        given(coursePort.findById(1L)).willReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.openCourse(3L, 1L))
                .isInstanceOf(RestApiException.class);
    }

    @Test
    @DisplayName("강의 삭제 성공 - DRAFT 상태 강의만 삭제 가능")
    void deleteCourse_success() {
        given(userPort.findById(1L)).willReturn(Optional.of(creator));
        given(coursePort.findById(1L)).willReturn(Optional.of(course));

        courseService.deleteCourse(1L, 1L);

        verify(coursePort).deleteById(1L);
    }

    @Test
    @DisplayName("강의 삭제 실패 - OPEN 상태 강의는 삭제 불가")
    void deleteCourse_fail_notDraft() {
        Course openCourse = Course.builder()
                .id(1L).creatorId(1L).title("Spring Boot 입문")
                .description("설명").price(50000).capacity(30).enrolledCount(0)
                .status(Course.Status.OPEN)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(40))
                .deadline(LocalDate.now().plusDays(7))
                .build();

        given(userPort.findById(1L)).willReturn(Optional.of(creator));
        given(coursePort.findById(1L)).willReturn(Optional.of(openCourse));

        assertThatThrownBy(() -> courseService.deleteCourse(1L, 1L))
                .isInstanceOf(RestApiException.class);
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}