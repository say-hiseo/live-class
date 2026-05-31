package com.example.enrollment.infrastructure.course;

import com.example.enrollment.domain.course.model.Course;
import com.example.enrollment.domain.course.port.out.CoursePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CourseRepositoryAdapter implements CoursePort {

    private final CourseJpaRepository courseJpaRepository;

    @Override
    public Course save(Course course) {
        CourseJpaEntity entity = courseJpaRepository.findById(course.getId() != null ? course.getId() : -1L)
                .map(existing -> {
                    existing.update(course);
                    return existing;
                })
                .orElse(CourseJpaEntity.fromDomain(course));
        return courseJpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Course> findById(Long id) {
        return courseJpaRepository.findById(id)
                .map(CourseJpaEntity::toDomain);
    }

    @Override
    public Optional<Course> findByIdWithLock(Long id) {
        return courseJpaRepository.findByIdWithLock(id)
                .map(CourseJpaEntity::toDomain);
    }

    @Override
    public List<Course> findAll(Course.Status status) {
        if (status == null) {
            return courseJpaRepository.findAll().stream()
                    .map(CourseJpaEntity::toDomain)
                    .collect(Collectors.toList());
        }
        return courseJpaRepository.findAllByStatus(status).stream()
                .map(CourseJpaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        courseJpaRepository.deleteById(id);
    }

    @Override
    public List<Course> findOpenCoursesWithExpiredDeadline() {
        return courseJpaRepository.findOpenCoursesWithExpiredDeadline(LocalDate.now()).stream()
                .map(CourseJpaEntity::toDomain)
                .collect(Collectors.toList());
    }
}