package com.example.enrollment.infrastructure.enrollment;

import com.example.enrollment.domain.enrollment.model.Waitlist;
import com.example.enrollment.domain.enrollment.port.out.WaitlistPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WaitlistRepositoryAdapter implements WaitlistPort {

    private final WaitlistJpaRepository waitlistJpaRepository;

    @Override
    public Waitlist save(Waitlist waitlist) {
        WaitlistJpaEntity entity = waitlistJpaRepository
                .findById(waitlist.getId() != null ? waitlist.getId() : -1L)
                .map(existing -> {
                    existing.update(waitlist);
                    return existing;
                })
                .orElse(WaitlistJpaEntity.fromDomain(waitlist));
        return waitlistJpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Waitlist> findById(Long id) {
        return waitlistJpaRepository.findById(id)
                .map(WaitlistJpaEntity::toDomain);
    }

    @Override
    public Optional<Waitlist> findWaitingByCourseIdAndUserId(Long courseId, Long userId) {
        return waitlistJpaRepository.findWaitingByCourseIdAndUserId(courseId, userId)
                .map(WaitlistJpaEntity::toDomain);
    }

    @Override
    public Optional<Waitlist> findFirstWaitingByCourseId(Long courseId) {
        return waitlistJpaRepository.findFirstWaitingByCourseId(courseId)
                .map(WaitlistJpaEntity::toDomain);
    }

    @Override
    public List<Waitlist> findAllWaitingByCourseId(Long courseId) {
        return waitlistJpaRepository.findAllWaitingByCourseId(courseId)
                .stream()
                .map(WaitlistJpaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int countWaitingByCourseId(Long courseId) {
        return waitlistJpaRepository.countWaitingByCourseId(courseId);
    }
}