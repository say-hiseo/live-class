package com.example.enrollment.global;

import com.example.enrollment.domain.course.model.Course;
import com.example.enrollment.domain.course.port.out.CoursePort;
import com.example.enrollment.domain.user.model.User;
import com.example.enrollment.domain.user.port.out.UserPort;
import com.example.enrollment.infrastructure.user.UserJpaEntity;
import com.example.enrollment.infrastructure.user.UserJpaRepository;
import com.example.enrollment.infrastructure.course.CourseJpaEntity;
import com.example.enrollment.infrastructure.course.CourseJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserJpaRepository userJpaRepository;
    private final CourseJpaRepository courseJpaRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userJpaRepository.count() > 0) {
            log.info("샘플 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        log.info("샘플 데이터 초기화 시작...");

        // 강사 생성
        UserJpaEntity creator1 = userJpaRepository.save(UserJpaEntity.builder()
                .name("김강사")
                .role(User.Role.CREATOR)
                .build());

        UserJpaEntity creator2 = userJpaRepository.save(UserJpaEntity.builder()
                .name("이강사")
                .role(User.Role.CREATOR)
                .build());

        // 수강생 생성
        UserJpaEntity student1 = userJpaRepository.save(UserJpaEntity.builder()
                .name("박수강")
                .role(User.Role.STUDENT)
                .build());

        UserJpaEntity student2 = userJpaRepository.save(UserJpaEntity.builder()
                .name("최수강")
                .role(User.Role.STUDENT)
                .build());

        UserJpaEntity student3 = userJpaRepository.save(UserJpaEntity.builder()
                .name("정수강")
                .role(User.Role.STUDENT)
                .build());

        // 강의 생성 (DRAFT)
        courseJpaRepository.save(CourseJpaEntity.builder()
                .creatorId(creator1.getId())
                .title("Spring Boot 입문")
                .description("Spring Boot 기초부터 실전까지")
                .price(50000)
                .capacity(30)
                .enrolledCount(0)
                .status(Course.Status.DRAFT)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(40))
                .deadline(LocalDate.now().plusDays(7))
                .build());

        // 강의 생성 (OPEN)
        courseJpaRepository.save(CourseJpaEntity.builder()
                .creatorId(creator1.getId())
                .title("JPA 실전")
                .description("JPA와 QueryDSL 실전 활용")
                .price(80000)
                .capacity(20)
                .enrolledCount(0)
                .status(Course.Status.OPEN)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(35))
                .deadline(LocalDate.now().plusDays(3))
                .build());

        // 강의 생성 (정원 1명짜리 - 동시성 테스트용)
        courseJpaRepository.save(CourseJpaEntity.builder()
                .creatorId(creator2.getId())
                .title("Redis 실전 (동시성 테스트용)")
                .description("정원 1명 강의 - 동시성 제어 테스트")
                .price(60000)
                .capacity(1)
                .enrolledCount(0)
                .status(Course.Status.OPEN)
                .startDate(LocalDate.now().plusDays(7))
                .endDate(LocalDate.now().plusDays(37))
                .deadline(LocalDate.now().plusDays(5))
                .build());

        log.info("샘플 데이터 초기화 완료");
        log.info("강사 - creator1 id: {}, creator2 id: {}", creator1.getId(), creator2.getId());
        log.info("수강생 - student1 id: {}, student2 id: {}, student3 id: {}",
                student1.getId(), student2.getId(), student3.getId());
    }
}