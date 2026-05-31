package com.example.enrollment.domain.enrollment.port.out;

import com.example.enrollment.domain.enrollment.model.Waitlist;

import java.util.List;
import java.util.Optional;

public interface WaitlistPort {
    Waitlist save(Waitlist waitlist);
    Optional<Waitlist> findById(Long id);
    Optional<Waitlist> findWaitingByCourseIdAndUserId(Long courseId, Long userId);
    Optional<Waitlist> findFirstWaitingByCourseId(Long courseId);
    List<Waitlist> findAllWaitingByCourseId(Long courseId);
    int countWaitingByCourseId(Long courseId);
}