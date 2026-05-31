package com.example.enrollment.domain.user.port.out;

import com.example.enrollment.domain.user.model.User;

import java.util.Optional;

public interface UserPort {
    Optional<User> findById(Long id);
}