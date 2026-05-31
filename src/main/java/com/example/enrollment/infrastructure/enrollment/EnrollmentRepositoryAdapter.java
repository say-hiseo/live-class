package com.example.enrollment.infrastructure.enrollment;

import com.example.enrollment.domain.enrollment.model.Enrollment;
import com.example.enrollment.domain.enrollment.port.out.EnrollmentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EnrollmentRepositoryAdapter implements EnrollmentPort {

    private final EnrollmentJpaRepository enrollmentJpaRepository;

    @Override
    public Enrollment save(Enrollment enrollment) {
        EnrollmentJpaEntity entity = enrollmentJpaRepository
                .findById(enrollment.getId() != null ? enrollment.getId() : -1L)
                .map(existing -> {
                    existing.update(enrollment);
                    return existing;
                })
                .orElse(EnrollmentJpaEntity.fromDomain(enrollment));
        return enrollmentJpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Enrollment> findById(Long id) {
        return enrollmentJpaRepository.findById(id)
                .map(EnrollmentJpaEntity::toDomain);
    }

    @Override
    public Optional<Enrollment> findActiveByCourseIdAndUserId(Long courseId, Long userId) {
        return enrollmentJpaRepository.findActiveByCourseIdAndUserId(courseId, userId)
                .map(EnrollmentJpaEntity::toDomain);
    }

    @Override
    public List<Enrollment> findAllByUserId(Long userId) {
        return enrollmentJpaRepository.findAllByUserId(
                        userId,
                        PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createdAt"))
                ).stream()
                .map(EnrollmentJpaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Enrollment> findAllByCourseId(Long courseId) {
        return enrollmentJpaRepository.findAllByCourseIdAndStatus(
                        courseId,
                        Enrollment.Status.CONFIRMED,
                        PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createdAt"))
                ).stream()
                .map(EnrollmentJpaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Enrollment> findExpiredPendingEnrollments() {
        return enrollmentJpaRepository.findExpiredPendingEnrollments(LocalDateTime.now().minusHours(24))
                .stream()
                .map(EnrollmentJpaEntity::toDomain)
                .collect(Collectors.toList());
    }
}